import pyorient

client = pyorient.OrientDB("localhost", 2480)
session_id = client.connect( "root", "telegram123")

client.db_open( "telegram", "root", "telegram123" )

