import cv2
import numpy as np


class homography:

    def __init__(self):
        self.sift = cv2.SIFT_create()
        index_params = dict(algorithm=1, trees=5)
        search_params = dict(checks=50)
        self.flann = cv2.FlannBasedMatcher(index_params, search_params)

    def homo(self, i1, i2):

        kp1, des1 = self.getSIFTFeatures(i1)
        kp2, des2 = self.getSIFTFeatures(i2)
        matches = self.flann.knnMatch(des2, des1, k=2)

        good_points = []

        for m, n in matches:
            if (m.distance < 0.8 * n.distance):
                good_points.append(m)

        if len(good_points) > 4:
            query_pts = np.float32([kp2[m.queryIdx].pt for m in good_points]).reshape(-1, 1, 2)

            train_pts = np.float32([kp1[m.trainIdx].pt for m in good_points]).reshape(-1, 1, 2)

            matrix, mask = cv2.findHomography(query_pts, train_pts, cv2.RANSAC, 4.0)
            return matrix

        return None

    def getSIFTFeatures(self, im):
        gray = cv2.cvtColor(im, cv2.COLOR_BGR2GRAY)
        kp, des = self.sift.detectAndCompute(gray, None)
        return kp, des
