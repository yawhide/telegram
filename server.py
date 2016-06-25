from flask import Flask, request
from pymongo import MongoClient, GEO2D
from bson.json_util import dumps,loads
import json
import pymongo

app = Flask(__name__)
app.debug = True
master_db = 'test_telgram2'
client = MongoClient()
db = client[master_db]
db.telegrams.create_index([("loc", '2dsphere')])
db.users.create_index([("uid,", pymongo.ASCENDING)])
db.expiry.create_index([("expiry", pymongo.ASCENDING)])

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

@app.route('/telegrams/seen', methods=['POST'])
def mark_telegram_seen():
  uid = request.form.get('uid')
  tid = request.form.get('tid')
  exp = request.form.get('exp')
  db.users.insert_one({'uid': uid, 'tid': tid})
  db.expiry.insert_one({'tid': tid, 'expiry': exp})
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
  uid_tele_set =  set([q['tid'] for q in seen_telegrams])

  query = {"loc": {"$geoWithin": {"$centerSphere": [[float(lng), float(lat)], float(rad)/3963.2 ]}}}
  cursor = db.telegrams.find(query).sort('_id')
  
  query_locked = {"loc": {"$geoWithin": {"$centerSphere": [[float(lng), float(lat)], (float(rad)+2)/3963.2 ]}}}
  cursor_locked = db.telegrams.find(query_locked).sort('_id')

  in_range_json = loads(dumps(cursor))
  locked_json = loads(dumps(cursor_locked))

  in_range_json = [q for q in in_range_json if str(q['_id']) not in uid_tele_set]
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

  telegram = Telegram (uid, msg, img, lat, lng)
  result = db.telegrams.insert_one(telegram.__dict__)
  return dumps(result.inserted_id)

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5000)
