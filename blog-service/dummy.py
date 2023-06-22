import threading
from pymongo import MongoClient
from faker import Faker
from elasticsearch import Elasticsearch
import time
import random

subjects = ['나', '너', '우리', '친구', '가족']
verbs = ['먹었습니다', '했습니다', '갔습니다', '본 적이 있습니다', '부족했습니다']
objects = ['밥', '게임', '책', '여행', '운동']



# mongodb
username = 'admin'
password = 'admin1234'
host = '192.168.0.45'
port = 27017
database_name = 'blog'
collection_name = 'posts'

uri = f"mongodb://{username}:{password}@{host}:{port}/{database_name}?authSource=admin&retryWrites=true&w=majority"
client = MongoClient(uri)
db = client[database_name]
collection = db[collection_name]

# es
elasticsearch_host = '192.168.0.45'
elasticsearch_port = 9200
index_name = 'post'
elasticsearch_client = Elasticsearch([{'host': elasticsearch_host, 'port': elasticsearch_port}])

fake = Faker("ko_KR")

def format_time(seconds):
    hours = seconds // 3600
    minutes = (seconds % 3600) // 60
    seconds = seconds % 60
    return f"{int(hours):02d}H {int(minutes):02d}M {int(seconds):02d}S"

def insert_data():
    start_time = time.time()
#     for _ in range(10000000):
    for _ in range(1000000):
        sentence = f"{random.choice(subjects)}는 오늘 {random.choice(objects)}을(를) {random.choice(verbs)}."
        data = {
            '_id': fake.uuid4(),
            'writerId': 'writer1',
            'isPrivate': False,
            'title': sentence,
#             'title': fake.sentence(),
            'content': {
                'text': sentence,
#                 'text': fake.paragraph(),
                'images': [{
                    'imageName': fake.word(),
                    'url': fake.url(),
                    'imageId': fake.uuid4()
                }],
                'videos': [{
                    'videoName': fake.word(),
                    'url': fake.url(),
                    'videoId': fake.uuid4()
                }]
            },
            'count': {
                'imageCount': 1,
                'videoCount': 1,
                'commentCount': 0
            },
            'created_at': fake.date_time_this_decade().strftime('%Y%m%d%H%M%S')
        }
        collection.insert_one(data)
        data["id"] = data["_id"]
        del data["_id"]
        elasticsearch_client.index(index=index_name, body=data)
    end_time = time.time()
    elapsed_time = end_time - start_time
    format_time = format_time(elapsed_time)
    print(f"Total save time: {format_time}")

thread_count = 4

threads = []
for _ in range(thread_count):
    t = threading.Thread(target=insert_data)
    threads.append(t)
    t.start()

for t in threads:
    t.join()

client.close()