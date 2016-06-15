from flask import Flask, request, jsonify
from pymongo import MongoClient, GEO2D

app = Flask(__name__)
app.debug = True
master_db = 'test_telgram'
client = MongoClient('localhost', 27017)
db = client[master_db]

class Telegram (object):
  def __init__ (self, tid, uid, msg, img, lat, lng):
    self.tid = tid
    self.uid = uid
    self.msg = msg
    self.img = img
    self.loc = {"lon": lng, "lat": lat}

@app.route("/")
def getAll():
  coll = db['telegrams']
  cursor = coll.find()
  for d in cursor:
    print d, d["_id"]
  return jsonify(results=[d for d in cursor])

@app.route('/drop', methods=['POST'])
def drop_telegram():
  tid = request.args.get('tid')
  uid = request.args.get('uid')
  msg = request.args.get('msg')
  img = request.args.get('img')
  lat = request.args.get('lat')
  lng = request.args.get ('lng')

  telegram = Telegram (tid, uid, msg, img, lat, lng)
  result = db.telegrams.insert_one(telegram.__dict__)
  print result.inserted_id
  # db.save(telegram.__dict__)

  return 'inserted telegram'

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5000)
