import dns.resolver
dns.resolver.default_resolver = dns.resolver.Resolver(configure=False)
dns.resolver.default_resolver.nameservers = ['8.8.8.8']
import numpy as np
import cv2
import time
from skimage.feature import local_binary_pattern
import os
import pymongo
from bson.binary import Binary
from os import listdir
from sklearn.svm import SVC
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score
from pymongo.mongo_client import MongoClient
from pymongo.server_api import ServerApi

myclient = pymongo.MongoClient("mongodb+srv://vidPro:vidPro@vidpro.wsmmizs.mongodb.net/")
mydb = myclient["vidPro"]
mycol = mydb["images"]

# Function to save an image in the database. The "team" parameter is a boolean indicating if it is from our team or not.
def saveImage(img, team):
    image_bin = cv2.imencode('.png', img)[1].tobytes()

    mydict = {
        "image": Binary(image_bin),
        "isTeam": team
    }

    x = mycol.insert_one(mydict)

    if x.inserted_id:
        print("Image successfully saved in MongoDB.")
    else:
        print("An error occurred while saving the image in MongoDB.")

# Function to retrieve all images from the database
def getImages():
    documents = mycol.find()
    images = []

    for document in documents:
        image_data = document["image"]
        nparr = np.frombuffer(image_data, np.uint8)
        image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        images.append(image)

    return images

# Function to detect a face using the camera
def detectFaceCamera():
    video_capture = cv2.VideoCapture(1)
    face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
    ret, frame = video_capture.read()
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    faces = face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(30, 30))

    for (x, y, w, h) in faces:
        face_image = frame[y:y + h, x:x + w]

    video_capture.release()
    cv2.destroyAllWindows()

    return face_image

# Function to detect a face in an image
def detectFaceImage(image_data):
    face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
    nparr = np.frombuffer(image_data, np.uint8)
    image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    faces = face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(30, 30))

    for (x, y, w, h) in faces:
        face_image = image[y:y + h, x:x + w]

    return face_image

# Function to get the pixel value at a given position in the image
def getPixel(img, center, x, y):
    new_value = 0
    try:
        if img[x][y] >= center:
            new_value = 1
    except:
        pass

    return new_value

# Function to compute the LBP value of a pixel in the image
def calculateLBP(img, x, y):
    center = img[x][y]
    value = []
    value.append(getPixel(img, center, x - 1, y - 1))
    value.append(getPixel(img, center, x - 1, y))
    value.append(getPixel(img, center, x - 1, y + 1))
    value.append(getPixel(img, center, x, y + 1))
    value.append(getPixel(img, center, x + 1, y + 1))
    value.append(getPixel(img, center, x + 1, y))
    value.append(getPixel(img, center, x + 1, y - 1))
    value.append(getPixel(img, center, x, y - 1))

    t = [1, 2, 4, 8, 16, 32, 64, 128]
    values = 0

    for i in range(len(value)):
        values += value[i] * t[i]

    return values

# Function to extract LBP features from all images in the database
def extractFeatures():
    images = getImages()
    images_lbp = []
    labels = []

    desired_size = (100, 100)

    for image in images:
        height, width, _ = image.shape
        face_image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        img_lbp = np.zeros((height, width), np.uint8)

        for i in range(0, height):
            for j in range(0, width):
                img_lbp[i, j] = calculateLBP(face_image, i, j)

        img_lbp_resized = cv2.resize(img_lbp, desired_size)
        img_lbp_flattened = img_lbp_resized.flatten()

        images_lbp.append(img_lbp_flattened)

    documents = mycol.find()
    for document in documents:
        labels.append(document["isTeam"])

    return images_lbp, labels

# Function to compare faces
def compareFaces(image_data):
    images_lbp, labels = extractFeatures()
    X = np.array(images_lbp)
    y = np.array(labels)
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    model = SVC(kernel='linear')
    model.fit(X_train, y_train)

    image = detectFaceImage(image_data)

    height, width, _ = image.shape
    face_image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    img_lbp = np.zeros((height, width), np.uint8)

    for i in range(0, height):
        for j in range(0, width):
            img_lbp[i, j] = calculateLBP(face_image, i, j)

    test_image = cv2.resize(img_lbp, (100, 100)).flatten()
    prediction = model.predict([test_image])

    if prediction in y_train:
        return True
    else:
        return False

def executeFaceRecognition(video_data):
    image = detectFaceImage(video_data)
    result = compareFaces(image)
    print(result)


# Test code
# video_data is the byte array representing the video
# result = executeFaceRecognition(video_data)
# print(result)
