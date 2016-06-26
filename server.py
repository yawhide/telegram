from flask import Flask, request
from pymongo import MongoClient, GEO2D
from bson.json_util import dumps,loads
import json
import pymongo

import boto
from boto.s3.key import Key
from uuid import uuid4


app = Flask(__name__)
app.debug = True
master_db = 'test_telgram5'
client = MongoClient()
db = client[master_db]
db.telegrams.create_index([("loc", '2dsphere')])
db.users.create_index([("uid", pymongo.ASCENDING), ("tid", pymongo.ASCENDING)], unique=True)
db.expiry.create_index([("expiry", pymongo.ASCENDING)])

AWS_ACCESS_KEY_ID = 'AKIAIVT3X6FRFZ4LP62Q'
AWS_SECRET_ACCESS_KEY = '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'

conn = boto.connect_s3(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
bucket = conn.get_bucket('telegramimages')
bucket.set_acl('public-read')


class Telegram (object):
  def __init__ (self, uid, msg, img, lat, lng):
    self.uid = uid
    self.msg = msg
    self.img = img
    self.loc = {"type":"Point", "coordinates": [float(lng),float(lat)]}

@app.route('/users/all', methods = ['GET'])
def get_users():
  cursor = db.users.find()
  return dumps(cursor)

@app.route('/telegrams/all', methods=['GET'])
def get_all():
  coll = db['telegrams']
  cursor = coll.find()
  return dumps(cursor)

# Really badly done right now. Must fix.
# Have created a compound index on uid, tid to prevent duplicates in our users db
# However, if a dup call is made, it throws an exception and doesn't insert
# Server keeps running
@app.route('/telegrams/seen', methods=['POST'])
def mark_telegram_seen():
  uid = request.form.get('uid')
  tid = request.form.get('tid')
  exp = request.form.get('exp')

  try:
    db.users.insert_one({'uid': uid, 'tid': tid})
    db.expiry.insert_one({'tid': tid, 'expiry': exp})
  except:
    pass

  return dumps(0)


def get_uid_telegrams(uid):
  cursor = db.users.find({"uid": uid})
  return loads(dumps(cursor))


# The radius is in miles | Unlockable : 1 mile, Observable : 2 miles
@app.route('/telegrams/within', methods=['GET'])
def telegrams_within ():
  lat = request.args.get('lat')
  lng = request.args.get ('lng')
  rad = request.args.get('rad')
  uid = request.args.get('uid')

  seen_telegrams = get_uid_telegrams (uid)

  query = {"loc": {"$geoWithin": {"$centerSphere": [[float(lng), float(lat)], float(rad)/3963.2 ]}}}
  cursor = db.telegrams.find(query).sort('_id')

  query_locked = {"loc": {"$geoWithin": {"$centerSphere": [[float(lng), float(lat)], (float(rad)+2)/3963.2 ]}}}
  cursor_locked = db.telegrams.find(query_locked).sort('_id')

  in_range_json = loads(dumps(cursor))
  locked_json = loads(dumps(cursor_locked))

  in_range = set([q['_id'] for q in in_range_json])
  out_of_range = [l for l in locked_json if  l['_id'] not in in_range]

  data = {}
  data['1'] = in_range_json
  data['2'] = out_of_range
  data['3'] = seen_telegrams

  return dumps(data)


@app.route('/drop', methods=['POST'])
def drop_telegram():
  uid = request.form.get('uid')
  msg = request.form.get('msg')
  img = request.form.get('img')
  lat = request.form.get('lat')
  lng = request.form.get ('lng')
  imgUrl = ''
  if (img):
    s3key = str(uuid4())
    k = Key(bucket)
    k.key = s3key
    k.set_contents_from_string(img)

    imgUrl = 'https://s3-us-west-2.amazonaws.com/telegramimages/' + s3key

  telegram = Telegram (uid, msg, imgUrl, lat, lng)
  result = db.telegrams.insert_one(telegram.__dict__)

  return dumps(result.inserted_id)

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5000)
