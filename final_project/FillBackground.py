import cv2
import math
import sys
import os
import argparse
import time
import shutil
import numpy as np

from tqdm import tqdm

class FillBackground:

    def mse(img1, img2):
        height, width, _ = img1.shape
        length = height * width
        diff = cv2.subtract(img1, img2)
        mse = np.sum(diff ** 2) / (float(length))
        return mse

    def FillBackground(bg, fgmasks, frames):
        print("log : Filling background...")
        frame_count, height, weight, channel = bg.shape
        # threshold value needs further test
        threshold = 10
        direction = np.array([[1, 0], [-1, 0], [0, 1], [0, -1]])
        sampleBG = []
        app3BG = []

        for a in tqdm(range(1, frame_count)):
            frame = frames[a]
            # for b in range(frame_count):
            #     if a != b and FillBackground.mse(bg[a], bg[b]) < threshold:
            #         for x in range(height):
            #             for y in range(weight):
            #                 # get sum of RGB value and check whether it's a black hole
            #                 colorA = bg[a][x][y][0] + bg[a][x][y][1] + bg[a][x][y][2]
            #                 colorB = bg[b][x][y][0] + bg[b][x][y][1] + bg[b][x][y][2]
            #                 # case 1: [x, y] in a is foreground and is a black hole and
            #                 # [x, y] in b is background and is not a black hole
            #                 # assign value of [x, y] in b to that of a and change its fgmasks value to 0
            #                 if fgmasks[a][x][y] and fgmasks[b][x][y] == 0 and colorA == 0 and colorB:
            #                     bg[a][x][y][0] = bg[b][x][y][0]
            #                     bg[a][x][y][1] = bg[b][x][y][1]
            #                     bg[a][x][y][2] = bg[b][x][y][1]
            #                     fgmasks[a][x][y] = 0
            #                 # case 2: [x, y] in a is background is not a black hole
            #                 # [x, y] in b is foreground and is a black hole
            #                 # assign value of [x, y] in a to that of b and change its fgmasks value to 0
            #                 elif fgmasks[a][x][y] == 0 and fgmasks[b][x][y] and colorA and colorB == 0:
            #                     bg[b][x][y][0] = bg[a][x][y][0]
            #                     bg[b][x][y][1] = bg[a][x][y][1]
            #                     bg[b][x][y][2] = bg[a][x][y][2]
            #                     fgmasks[b][x][y] = 0
            for x in range(height):
                for y in range(weight):

                    if fgmasks[a][x][y]:
                        count = 0
                        tmpX = x
                        tmpY = y
                        tmpB = 0
                        tmpG = 0
                        tmpR = 0
                        for k in range(len(direction)):
                            while(tmpX >= 0 and tmpX < height and tmpY >= 0 and tmpY < weight and fgmasks[a][tmpX][tmpY]):
                                tmpX = tmpX + direction[k][0]
                                tmpY = tmpY + direction[k][1]
                            if(tmpX >= 0 and tmpX < height and tmpY >= 0 and tmpY < weight):
                                # the following code will cause 'RuntimeWarning: overflow encountered in ubyte_scalars' error:
                                # bg[a][x][y][0] += bg[a][tmpX][tmpY][0]
                                # bg[a][x][y][1] += bg[a][tmpX][tmpY][1]
                                # bg[a][x][y][2] += bg[a][tmpX][tmpY][2]
                                # count += 1

                                tmpB += bg[a][tmpX][tmpY][0]
                                tmpG += bg[a][tmpX][tmpY][1]
                                tmpR += bg[a][tmpX][tmpY][2]
                                count += 1
                        if count:
                            bg[a][x][y][0] = tmpB / count
                            bg[a][x][y][1] = tmpG / count
                            bg[a][x][y][2] = tmpR / count

            app3BG.append(bg[a])
            if (a - 1) % 8 == 0:
                sampleBG.append(bg[a])
        return sampleBG, app3BG
