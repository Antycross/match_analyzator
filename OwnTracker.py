import os
import cv2
import numpy as np
import bluetooth as bt
import logging
import time


def find_nearest_player(frm, ball_x, ball_y, team_color, plain):
    imgHSV = cv2.cvtColor(frm, cv2.COLOR_BGR2HSV_FULL)
    lower = np.array(team_color[0:3])
    upper = np.array(team_color[3:6])
    mask = cv2.inRange(imgHSV, lower, upper)
    kernel = np.ones((5, 5), np.uint8)
    mask = cv2.dilate(mask, kernel, iterations=1)
    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel)

    if plain == 1:
        lower_parameter = 233
        upper_parameter = 1300
    else:
        lower_parameter = 150
        upper_parameter = 517

    contours, hierarchy = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_NONE)
    distance_to_the_ball = []
    for cnt in contours:
        area = cv2.contourArea(cnt)
        arcLength = cv2.arcLength(cnt, True)
        approx = cv2.approxPolyDP(cnt, 0.02 * arcLength, True)
        if lower_parameter < area < upper_parameter:
            x, y, w, h = cv2.boundingRect(approx)
            distance_to_the_ball.append(np.sqrt(np.power(ball_x - x, 2) + np.power(ball_y - y, 2)))
    if distance_to_the_ball:
        min_distance = min(distance_to_the_ball)
        return min_distance
    else:
        return -1


def detect_lines(frm, my_color):
    imgHSV = cv2.cvtColor(frm, cv2.COLOR_BGR2HSV_FULL)
    lower = np.array(my_color[0:3])
    upper = np.array(my_color[3:6])
    mask = cv2.inRange(imgHSV, lower, upper)
    edges = cv2.Canny(mask, 50, 50)

    rho = 1  # check every 1 pixel
    theta = np.pi / 180  # check every degree
    threshold = 15  # minimum number of intersections to decide that is line
    min_line_length = 50  # minimum number of pixels making up a line
    max_line_gap = 20  # maximum gap in pixels between connectable line segments

    # Run Hough on edge detected image
    # Output "lines" is an array containing endpoints of detected line segments
    lines = cv2.HoughLinesP(edges, rho, theta, threshold, np.array([]),
                            min_line_length, max_line_gap)

    middle_line = [0, 0, 0, 0]
    side_line = [0, 0, 0, 0]
    another_lines = []
    print("lines")
    if lines is not None:
        for line in lines:
            line_detect = False
            for x1, y1, x2, y2 in line:
                length = np.sqrt(np.power(x2 - x1, 2) + np.power(y2 - y1, 2))
                if length > 400:
                    if (370 < x1 < 830 or 370 < x2 < 830) and (
                            abs(x2 - x1) < abs(y2 - y1)):  # zawężenie obszaru linii środkowej, eliminacja pasków
                        middle_line = [x1, y1, x2, y2]
                        print("middle")
                        line_detect = True
                    if abs(x2 - x1) > abs(y2 - y1) and length > 770:
                        side_line = [x1, y1, x2, y2]
                        print("side")
                        line_detect = True
                    if not line_detect:
                        print("another")
                        another_line = [x1, y1, x2, y2]
                        another_lines.append(another_line)
                    print(length, x1, y1, x2, y2)
    middle_line_detect = middle_line[0] + middle_line[1] + middle_line[2] + middle_line[3]
    side_line_detect = side_line[0] + side_line[1] + side_line[2] + side_line[3]
    if middle_line_detect != 0 and side_line_detect != 0:
        return 4, middle_line, side_line
    elif middle_line_detect != 0:
        return 1, middle_line, [0, 0, 0, 0]
    elif side_line_detect:
        return 2, [0, 0, 0, 0], side_line
    elif another_lines:
        return 3, another_lines, [0, 0, 0, 0]
    else:
        return 0, [0, 0, 0, 0], [0, 0, 0, 0]


def which_side(ball_x, ball_y, situation, position_side,
               position_zone):  # -1 - left 0 - undef 1 - right # -1 - down 0 - center 1 - up
    horizontal = 0
    vertical = 0
    if situation == 1 or situation == 4:  # middle_line detected
        if ball_x > position_side[0] and ball_x > position_side[2]:
            horizontal = 1
        elif ball_x < position_side[0] and ball_x < position_side[2]:
            horizontal = -1
        elif position_side[0] > position_side[2] and ball_x > position_side[2]:
            horizontal = 1
        elif position_side[2] > position_side[0] and ball_x > position_side[0]:
            horizontal = 1
        else:
            horizontal = -1
    if situation == 2 or situation == 4:  # side_line detected
        if ball_y > position_zone[1] and ball_y > position_zone[3]:
            vertical = 1
        elif ball_y < position_zone[1] and ball_y < position_zone[3]:
            vertical = -1
        elif ball_y < 400:
            vertical = 1
        else:
            vertical = -1
    return horizontal, vertical


def find_ball(frm, my_color, previous_position):
    img_hsv = cv2.cvtColor(frm, cv2.COLOR_BGR2HSV_FULL)
    lower = np.array(my_color[0:3])
    upper = np.array(my_color[3:6])
    mask = cv2.inRange(img_hsv, lower, upper)
    kernel = np.ones((3, 3), np.uint8)
    mask = cv2.bitwise_not(mask)
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel)
    mask = cv2.dilate(mask, kernel, iterations=1)
    canny_frame = cv2.Canny(mask, 50, 50)
    contours, hierarchy = cv2.findContours(canny_frame, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_NONE)
    number_of_ball_position = 0
    list_of_position = []
    for cnt in contours:
        area = cv2.contourArea(cnt)
        arc_length = cv2.arcLength(cnt, True)
        approx = cv2.approxPolyDP(cnt, 0.02 * arc_length, True)
        if 87 < area < 200 and arc_length <= 42.97:
            x, y, w, h = cv2.boundingRect(approx)
            number_of_ball_position = number_of_ball_position + 1
            list_of_position.append([x, y, w, h])

    if previous_position[0] == 0:
        if number_of_ball_position == 0:
            return [0, 0, 0, 0]
        else:  # return first searched position
            return list_of_position[0]
    else:
        if number_of_ball_position == 0:
            return [0, 0, 0, 0]
        elif number_of_ball_position == 1:
            return list_of_position[0]
        else:
            actual_pos = 0
            prev_x = previous_position[0] + previous_position[2] / 2
            prev_y = previous_position[1] + previous_position[3] / 2
            x_pos = list_of_position[0][0] + list_of_position[0][2] / 2
            y_pos = list_of_position[0][1] + list_of_position[0][3] / 2
            smallest_difference = abs(prev_x - x_pos) + abs(prev_y - y_pos)
            for i, pos in enumerate(list_of_position[1:]):
                x_pos = pos[0] + pos[2] / 2
                y_pos = pos[1] + pos[3] / 2
                difference = abs(prev_x - x_pos) + abs(prev_y - y_pos)
                if difference < smallest_difference:
                    smallest_difference = difference
                    actual_pos = i
        return list_of_position[actual_pos]


def confirm(sock, status):
    if status:
        sock.send("1".encode("utf_8"))
        print("correct")
    else:
        sock.send("0".encode("utf_8"))
        print("error")


def bluetooth_communication(sock):
    my_colors = []
    data = []
    plain = []
    is_plain_color_write = False
    while True:
        try:
            if len(my_colors) >= 2 and is_plain_color_write is False:
                d = sock.recv(3).decode("utf_8")
                print("d plain_color", int(d))
                plain.append(int(d))
                print("plain_color", plain)
                if d is None:
                    confirm(sock, False)
                    plain.clear()
                else:
                    confirm(sock, True)
                    is_plain_color_write = True
            d = sock.recv(3).decode("utf_8")
            print("d", int(d))
            if d is None:
                confirm(sock, False)
            else:
                confirm(sock, True)
                data.append(int(d))
            if len(data) > 5:
                print("Received", data)
                my_colors.append(data)
                print("my_colors", my_colors, len(my_colors))
                confirm(sock, True)
                data = []
                is_plain_color_write = False
            if len(my_colors) > 3:
                break
        except OSError:
            pass

    print("my_colors", my_colors)
    print("plain_color", plain)

    print("Receiving video")
    if os.path.isfile("match_video.mp4"):
        os.remove("match_video.mp4")
    f = open("match_video.mp4", "wb")
    while True:
        try:
            sock.settimeout(20)
            d = sock.recv(10).decode("utf_8")
            if d is None:
                confirm(sock, False)
            elif int(d) < 2:
                confirm(sock, False)
            else:
                confirm(sock, True)
                print("video d length", int(d))
                all_bytes = int(d)
                read_bytes = all_bytes
                sock.recv(10).decode("utf_8")
                while read_bytes > 0:
                    amount_of_bytes = min(1024, read_bytes)
                    b = sock.recv(amount_of_bytes)
                    if not b:
                        break
                    if b is None:
                        confirm(sock, False)
                    else:
                        confirm(sock, True)
                        print("video d", str(b), len(b), int((1 - read_bytes / all_bytes) * 100), read_bytes, all_bytes)
                        f.write(b)
                        print("len decode", len(b))
                        read_bytes = read_bytes - len(b)
                f.close()
                break
        except OSError as e:
            logger.error(str(e), exc_info=True)
            f.close()
            sock.send("3".encode("utf_8"))
            break
        except ValueError:
            pass
    print("Receive video")

    return my_colors, plain


def send_results(ball_lost, sock):
    length = str(len(ball_lost) * 3).encode("utf_8")
    print(len(ball_lost))
    print(length)
    sent = False
    while not sent:
        sock.send(length)
        sent = response(sock)

    print(ball_lost)
    for i in ball_lost:
        print("i", i)
        for value in i:
            sent = False
            while not sent:
                sock.send(str(value).encode("utf_8"))
                print(value)
                sent = response(sock)


def response(sock):
    r = None
    t1 = time.time()
    while r is None:
        if time.time() - t1 > 10:
            break
        r = sock.recv(1).decode("utf_8")
    if r == "1":
        return True
    else:
        return False


def ready(sock):
    sent = False
    while not sent:
        sock.send("2".encode("utf_8"))
        sent = response(sock)


server_sock = bt.BluetoothSocket(bt.RFCOMM)
server_sock.bind(("", bt.PORT_ANY))
server_sock.listen(1)

logger = logging.getLogger()
logger.setLevel(logging.DEBUG)

port = server_sock.getsockname()[1]

uuid = "58723436-5452-11eb-ae93-0242ac130002"

bt.advertise_service(server_sock, "SampleServer", service_id=uuid,
                     service_classes=[uuid, bt.SERIAL_PORT_CLASS],
                     profiles=[bt.SERIAL_PORT_PROFILE],
                     )
while True:
    print("Waiting for connection on RFCOMM channel", port)

    client_sock, client_info = server_sock.accept()
    print("Accepted connection from", client_info)

    colors, plain_color = bluetooth_communication(client_sock)
    path = 'match_video2.mp4'
    video = cv2.VideoCapture(path)

    FrameCount = 0
    position = [0, 0, 0, 0]
    actual_side = 0  # 1 - left, 0 - undef, -1 - right
    actual_zone = 0  # 1 - up 0 - central -1 - down
    up = 0
    central = 0
    down = 0
    line_detected = 0
    first_team_at_the_ball = 0
    second_team_at_the_ball = 0
    actual_zone_after_voting = 0
    actual_team_at_the_ball = 0
    ball_lost1 = []
    ball_lost2 = []

    colors = [[56, 97, 20, 86, 182, 255],
              [56, 14, 96, 156, 116, 255],
              [237, 111, 68, 265, 255, 255],
              [0, 180, 90, 25, 228, 255]]  # FM1
    # colors = [[46, 87, 76, 91, 180, 255],
    #           [65, 0, 71, 184, 111, 255],
    #           [224, 50, 2, 272, 225, 255],
    #           [60, 0, 63, 214, 83, 255]]  # FM2
    # colors = [[52, 126, 47, 65, 197, 127],
    #           [65, 14, 84, 172, 126, 255],
    #           [239, 64, 105, 271, 255, 255],
    #           [234, 74, 102, 268, 255, 241]]  # FM3
    # colors = [[44, 150, 105, 66, 238, 170],
    #           [51, 34, 173, 83, 143, 245],
    #           [36, 96, 200, 51, 145, 255],
    #           [0, 0, 0, 37, 232, 255]]  # real_video
    # pitch color, lines color, 1st team color, 2nd team color
    plain_color = [0, 1]  # FM1
    # plain_color = [0, 1]  # FM2
    # plain_color = [1, 0]  # FM3
    # plain_color = [0, 1]  # real_video
    # the same color of T-shirts and shorts

    while True:

        FrameCount = FrameCount + 1
        ret, frame = video.read()
        if frame is None:
            print("ball_lost1", ball_lost1)
            print("ball_lost2", ball_lost2)
            break
        frame = cv2.resize(frame, (1200, 800))

        print("frame_count", FrameCount)
        position = find_ball(frame, colors[0], position)

        if FrameCount % 30 == 0:

            if first_team_at_the_ball > second_team_at_the_ball:
                if actual_team_at_the_ball == -1:
                    ball_lost2.append([actual_zone_after_voting, actual_side, int(round(FrameCount / 30))])
                print("First team at the ball")
                actual_team_at_the_ball = 1
            elif first_team_at_the_ball < second_team_at_the_ball:
                if actual_team_at_the_ball == 1:
                    ball_lost1.append([actual_zone_after_voting, actual_side, int(round(FrameCount / 30))])
                print("Second team at the ball")
                actual_team_at_the_ball = -1
            elif actual_team_at_the_ball == 1:
                print("First team at the ball")
            elif actual_team_at_the_ball == -1:
                print("Second team at the ball")
            else:
                print("Undef")

            if actual_side == -1:
                print("left")
            elif actual_side == 1:
                print("right")
            else:  # actual side = 0
                print("undef")
            if up >= central and up > down:
                print("up")
                actual_zone_after_voting = 1
            elif down >= central and down > up:
                print("down")
                actual_zone_after_voting = -1
            else:
                if line_detected == 1:  # some line detected
                    print("central")
                    actual_zone_after_voting = 0
                else:
                    if actual_zone_after_voting == 1:
                        print("up")
                    elif actual_zone_after_voting == -1:
                        print("down")
                    else:
                        print("central")
            up = 0
            down = 0
            central = 0
            line_detected = 0
            first_team_at_the_ball = 0
            second_team_at_the_ball = 0

        if position[0] + position[1] + position[2] + position[3] != 0:
            print("pilka", position)
            line_situation, line_position_side, line_position_zone = detect_lines(frame, colors[1])

            if line_situation != 0:

                line_detected = 1
                hor, ver = which_side(position[0], position[1], line_situation, line_position_side, line_position_zone)
                if hor != 0:
                    actual_side = hor
                if actual_side == -1:
                    print("left")
                if actual_side == 1:
                    print("right")
                if actual_side == 0:
                    print("undef")

                actual_zone = ver
                if actual_zone == -1:
                    down = down + 1
                    print("down", down)
                if actual_zone == 1:
                    up = up + 1
                    print("up", up)
                if actual_zone == 0:
                    central = central + 1
                    print("central", central)

            nearest_player_first_team = find_nearest_player(frame, position[0], position[1], colors[2],
                                                            plain_color[0])
            nearest_player_second_team = find_nearest_player(frame, position[0], position[1], colors[3],
                                                             plain_color[1])

            if (nearest_player_first_team != -1 or nearest_player_second_team != -1) and (
                    nearest_player_first_team < 50 or nearest_player_second_team < 50):
                string1 = " 1 " + str(nearest_player_first_team)
                print(string1)
                string2 = " 2 " + str(nearest_player_second_team)
                print(string2)
                if nearest_player_first_team < nearest_player_second_team:  # distance from ball
                    first_team_at_the_ball = first_team_at_the_ball + 1
                    print(first_team_at_the_ball)
                else:
                    second_team_at_the_ball = second_team_at_the_ball + 1
                    print(second_team_at_the_ball)
        key = cv2.waitKey(5) & 0xFF
        if key == ord('q'):
            break

    ready(client_sock)
    print("ready state complete")
    send_results(ball_lost1, client_sock)
    print("send results1 complete")
    send_results(ball_lost2, client_sock)
    print("send results2 complete")

    video.release()
    cv2.destroyAllWindows()
