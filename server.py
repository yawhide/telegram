from flask import Flask, request
from pymongo import MongoClient, GEO2D
from bson.json_util import dumps,loads
import json

app = Flask(__name__)
app.debug = True
master_db = 'test_telgram2'
client = MongoClient()
db = client[master_db]
db.telegrams.create_index([("loc", '2dsphere')])

class Telegram (object):
  def __init__ (self, uid, msg, img, lat, lng):
    self.uid = uid
    self.msg = msg
    self.img = img
    print lat, lng
    self.loc = {"type":"Point", "coordinates": [float(lng),float(lat)]}

@app.route('/telegrams/all', methods=['GET'])
def getAll():
  coll = db['telegrams']
  cursor = coll.find()
  return dumps(cursor)

# The radius has to be in miles -- currently defaulted to 1 mile 
# 200m radius might be too small. Unlockable : 1 mile, Observable : 2 miles
@app.route('/telegrams/within', methods=['GET'])
def getTelegramsWithin ():
  lat = request.args.get('lat')
  lng = request.args.get ('lng')
  rad = request.args.get('rad')
  query = {"loc": {"$geoWithin": {"$centerSphere": [[float(lng), float(lat)], float(rad)/3963.2 ]}}}
  cursor = db.telegrams.find(query).sort('_id')
  
  query_locked = {"loc": {"$geoWithin": {"$centerSphere": [[float(lng), float(lat)], (float(rad)+1)/3963.2 ]}}}
  cursor_locked = db.telegrams.find(query_locked).sort('_id')

  in_range_json = loads(dumps(cursor))
  locked_json = loads(dumps(cursor_locked))

  in_range = set([q['_id'] for q in in_range_json])
  out_of_range = [l for l in locked_json if  l['_id'] not in in_range]

  data = {}
  data['1'] = in_range_json
  data['2'] = out_of_range

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
