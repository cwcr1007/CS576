import argparse
import cv2
import numpy as np
import os
from imutils.object_detection import non_max_suppression
from tqdm import tqdm
from FillBackground import FillBackground
from homography import homography

def args_parser():
    parser = argparse.ArgumentParser()
    parser.add_argument('--v', type=str, required=True)
    args = parser.parse_args()
    return args

def read_video(cam):
    frames = []

    while True:
        ret, frame = cam.read()

        if ret:
            frames.append(frame)
        else:
            break

    return np.array(frames)

def dense_optical_flow(frames):
    cur_frame = cv2.cvtColor(frames[0], cv2.COLOR_BGR2GRAY)
    frame_hsv = np.zeros_like(frames[0])
    frame_hsv[..., 1] = 255
    foreground_masks = []
    foreground_masks.append(np.zeros(frames[0].shape[:2], np.uint8))
    white_fgs = []
    for i in tqdm(range(1, len(frames))):
        frame = frames[i]
        next_frame = cv2.cvtColor(frames[i], cv2.COLOR_BGR2GRAY)

        optical_flow = cv2.calcOpticalFlowFarneback(cur_frame, next_frame, None, 0.5, 3, 50, 3, 5, 1.5, 256)

        magnitude, angle = cv2.cartToPolar(optical_flow[..., 0], optical_flow[..., 1])
        frame_hsv[..., 0] = angle * 180 / np.pi / 2
        frame_hsv[..., 2] = cv2.normalize(magnitude, None, 0, 255, cv2.NORM_MINMAX)
        frame_bgr = cv2.cvtColor(frame_hsv, cv2.COLOR_HSV2BGR)

        (threshold, foreground_mask) = cv2.threshold(frame_bgr[:, :, 2:3], 128, 255,  cv2.THRESH_BINARY | cv2.THRESH_OTSU)
        k = cv2.waitKey(30) & 0xff

        foreground_mask[foreground_mask > 0] = 1
        cur_frame = next_frame
        foreground_masks.append(foreground_mask)

        foreground = frame * foreground_mask[:, :, np.newaxis]
        foreground[foreground_mask == 0] = (255, 255, 255)
        white_fgs.append(foreground)

    return np.array(foreground_masks), white_fgs

def generate_panorama(background):
    print("stitching ...")

    stitchy = cv2.Stitcher.create()
    (dummy, output) = stitchy.stitch(background)

    if dummy != cv2.STITCHER_OK:
        print("stitching ain't successful")
    else:
        print('Your Panorama is ready!!!')

    return output

def merge_foreground(background, foreground, fps, all_frames):
    print('merging panorama and foreground...')

    frames = []
    out1 = background.copy()
    n = 1.5
    homo = homography()

    height, width, _ = background.shape
    cam = cv2.VideoWriter(f'pre_out2.mp4', cv2.VideoWriter_fourcc(*'mp4v'), fps, (width, height))

    for i in tqdm(range(foreground.shape[0])):
        H = homo.homo(background, all_frames[i])
        if H is None:
            frames.append(all_frames[-1])
            continue
        h, w = background.shape[0], background.shape[1]
        overlay_foreground = cv2.warpPerspective(foreground[i], H, (w, h))
        frame = overlay(background, overlay_foreground)
        frames.append(frame)

        cam.write(frame)

        if i % (fps * n) == 0:
            out1 = overlay(out1, overlay_foreground)

    cam.release()

    return np.array(frames), out1

def overlay(image: np.ndarray, overlay: np.ndarray, background_LB=np.array([0, 0, 0]), background_UB=np.array([5, 5, 5])) -> np.ndarray:
    mask = cv2.inRange(overlay, background_LB, background_UB)
    masked_image = cv2.bitwise_and(image, image, mask=mask)
    return cv2.bitwise_or(overlay, masked_image)

def generate_new_video(pano, frames_foreground_in_pano):
    print("log : go generate_new_video")

    new_height = 1080
    new_weidth = 1800
    new_frames_count = 631
    new_fps = 30

    start = [600, 1000]
    end = [600, 3850]

    dx = int((end[0] - start[0]) / new_frames_count)
    dy = int((end[1] - start[1]) / new_frames_count)

    new_video = []
    center = start
    for i in tqdm(range(new_frames_count)):
        frame = frames_foreground_in_pano[i]
        left_x = center[0] - int(new_height / 2)
        right_x = center[0] + int(new_height / 2)
        up_y = center[1] - int(new_weidth / 2)
        bottom_y = center[1] + int(new_weidth / 2)
        new_frame = frame[left_x:right_x, up_y:bottom_y]

        cv2.imshow('new_frame', new_frame)
        k = cv2.waitKey(30) & 0xff
        if k == 27:
            break

        new_video.append(new_frame)
        center[0] += dx
        center[1] += dy

    out2 = cv2.VideoWriter(f'out2.mp4', cv2.VideoWriter_fourcc(*'mp4v'), new_fps, (new_weidth, new_height))
    for fg in new_video:
        out2.write(fg)
    out2.release()

    return new_video

def main(args):
    cam = cv2.VideoCapture(args.v)

    width = int(cam.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cam.get(cv2.CAP_PROP_FRAME_HEIGHT))
    fps = cam.get(cv2.CAP_PROP_FPS)

    all_frames = read_video(cam)

    # step1: extract foreground
    foreground_masks, white_fgs = dense_optical_flow(all_frames)
    print("log : completed get_foreground_mask_dof")

    # intermediary output 2
    inter2 = cv2.VideoWriter(f'inter2.mp4', cv2.VideoWriter_fourcc(*'mp4v'), fps, (width, height))
    for fg in white_fgs:
        inter2.write(fg)
    inter2.release()

    background_masks = np.where((foreground_masks == 1), 0, 1).astype('uint8')
    background = all_frames * background_masks[:, :, :, np.newaxis]
    foreground = all_frames * foreground_masks[:, :, :, np.newaxis]

    pano_file = f'pano.jpg'
    if not os.path.exists(pano_file):
        # step2: fill background
        filled_BG, app3_BG = FillBackground.FillBackground(background, foreground_masks, all_frames)
        print("log : completed fillBackground")

        # application output 3
        out3 = cv2.VideoWriter(f'out3.mp4', cv2.VideoWriter_fourcc(*'mp4v'), fps, (width, height))
        for bg in app3_BG:
            out3.write(bg)
        out3.release()

        # step 3: generate panorama -> intermemidary output 1
        panorama = generate_panorama(filled_BG)
        print("log : completed generate_panorama")
        cv2.imwrite(pano_file, panorama)
    else:
        print('Cached panorama file is used.')

    read_panorama = cv2.imread(pano_file)

    pre_out2_file = f'pre_out2.mp4'
    if not os.path.exists(pre_out2_file):
        res, out1 = merge_foreground(read_panorama, foreground, int(fps), all_frames)
        # step 4: application output 1
        cv2.imwrite(f'out1.jpg', out1)
        print("log : completed merge_foreground")
    else:
        cam = cv2.VideoCapture(pre_out2_file)
        res = read_video(cam)
        print('Cached pre_out2.mp4 file is used.')

    # if pre_out2.mp4 exists
    generate_new_video(read_panorama, res)
    print("log : completed generate_new_video")

    cam.release()
    cv2.destroyAllWindows()


if __name__ == "__main__":
    args = args_parser()
    main(args)
