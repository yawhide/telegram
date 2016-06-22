from flask import Flask, request
from pymongo import MongoClient, GEO2D
from bson.json_util import dumps

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
  return dumps(cursor) 


@app.route('/drop', methods=['POST'])
def drop_telegram():
  uid = request.args.get('uid')
  msg = request.args.get('msg')
  img = request.args.get('img')
  lat = request.args.get('lat')
  lng = request.args.get ('lng')

  telegram = Telegram (uid, msg, img, lat, lng)
  result = db.telegrams.insert_one(telegram.__dict__)
  print result.inserted_id
  return 'inserted telegram'

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5000)
