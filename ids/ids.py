import argparse
from time import time
from datetime import datetime
import os
import random
import sys
import signal
import socket
import threading

port_server = 10000
port_client = 9999
max_buf_size = 1380


def interrupt_handler(signal, frame):
    """
    Handle the interrupt signal and gracefully shut down the server.

    :param signal:
    :param frame:
    :return:
    """
    print("Shutting down IDS")
    sys.exit(0)


def read_patterns(filename="pattern_config.txt"):
    """
    one row of pattern configuration file is like -- pattern_id, pattern_in_hex
    """
    try:
        with open(filename, "r") as f:
            p_list = f.readlines()
            if len(p_list) is 1:
                row = p_list.pop(0)
                # check first row's format, if wrong, exit
                str1, str2 = row.split('\t')
                if str1 != "pattern_id" or str2 != "pattern_in_hex\n":
                    print("wrong configuration file format.\ndo like 'pattern_id  pattern_in_hex', where a tab separates the id and pattern")
                    exit(1)
            elif len(p_list) > 1:
                # save patterns in a dict, pattern(bytes) is key, id(string) is value
                p_dict = dict()
                for p in p_list:
                    pid, pattern_hex = p.split('\t')
                    pattern_hex, _ = pattern_hex.split('\n')
                    pattern = bytes.fromhex(pattern_hex)
                    p_dict[pattern] = pid
                return p_dict
            else:
                print("Warning: You have not declared any patterns.")
    except IOError:
        print("configuration file doesn't exist")
        exit(1)


def detect_patterns(p_dict, data, ip, log):
    """
    detect patterns, if yes, log info and return True; else, return False

    :param p_dict: 
    :param data: 
    :param ip: 
    :return: 
    """

    if p_dict is None:
        return False

    # go through each pattern in p_dict
    for p in p_dict:
        if p in data:
            timestamp = datetime.fromtimestamp(time()).strftime('%Y-%m-%d %H:%M:%S')
            # log pattern id, source ip, timestamp
            info = p_dict[p] + '\t' + ip + '\t' + timestamp + '\n'
            with open(log, "a") as f:
                f.write(info)
            return True
    return False


def client_to_server(sock_client, sock_server, client_ip, p_dict, log):
    """
    A thread to receive data from client, check pattern, send data to server

    :param sock_client:
    :param sock_server:
    :param client_ip:
    :param p_dict:
    :return:
    """
    try:
        while True:
            data = sock_client.recv(max_buf_size)
            if data:
                # detect patterns, if no match, send it to server; else, drop
                if not detect_patterns(p_dict, data, client_ip, log):
                    try:
                        sock_server.sendall(data)
                    except Exception:
                        print("server is closed")
                        break
                else:
                    print("pattern detected")
            # EOF from client means client is closed, close socket with client
            else:
                break
    # ctrl+c when waiting for pending data
    except KeyboardInterrupt:
        print('\nids closed')
    finally:
        # clean up the connection with client
        sock_client.close()


def server_to_client(sock_server, sock_client, server_ip, p_dict, log):
    """
    a thread to receive data from server, check pattern, send data to client

    :param sock_server:
    :param sock_client:
    :param server_ip:
    :param p_dict:
    :return:
    """
    try:
        while True:
            data = sock_server.recv(max_buf_size)
            if data:
                # detect patterns, if no match, send it to server; else, drop
                if not detect_patterns(p_dict, data, server_ip, log):
                    try:
                        sock_client.sendall(data)
                    except Exception:
                        print("client is closed")
                        break
                else:
                    print("pattern detected")
            # EOF from server means server is closed, close socket with server
            else:
                break
    # ctrl+c when waiting for pending data
    except KeyboardInterrupt:
        print('\nids closed')
    finally:
        # clean up the connection with server
        sock_server.close()


if __name__ == '__main__':
    signal.signal(signal.SIGINT, interrupt_handler)

    # read pattern from configuration file in the command line
    parser = argparse.ArgumentParser()
    parser.add_argument("config", help="pattern configuration file")
    parser.add_argument("listen_port", help="the port to listen for client connections")
    parser.add_argument("server_port", help="the port to connect to the server")
    parser.add_argument("log", help="log file")
    args = parser.parse_args()
    config, port_client, port_server, log = args.config, args.listen_port, args.server_port, args.log

    if port_client.isdigit() and port_server.isdigit():
        if int(port_client) < 1 and int(port_client) > 65535:
            print("Invalid listening port number")
            exit(1)

        if int(port_server) < 1 and int(port_server) > 65535:
            print("Invalid server port number.")
            exit(1)
    else:
        print("You must use a valid port number (1-65535)")
        exit(1)

    p_dict = read_patterns(config)

    # get connection with server
    sock_server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    # use loopback interface
    server_address_lb = ('localhost', int(port_server))

    try:
        sock_server.connect(server_address_lb)
    except ConnectionRefusedError:
        print("Server could not be reached")
        sock_server.close()
        exit(1)
    except:
        print("Unknown server connection error")
        exit(1)

    try:
        # listening for client
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        # use ethernet address
        server_ip = socket.gethostbyname(socket.gethostname())
        server_address_en = (server_ip, int(port_client))
        print("starting up on %s %s" % server_address_en)
        sock.bind(server_address_en)
        sock.listen(1)

        sock_client, client_address = sock.accept()
        print("connection from %s %s" % client_address)
        client_ip, _ = client_address
    except OSError:
        print("Could not start IDS server.")
        exit(1)
    # ctrl+c when waiting for connection with client
    except KeyboardInterrupt:
        print('\nids closed')
        exit(1)
    except:
        print("Unknown IDS error. Exiting.")
        exit(1)

    # use two threads to handle two pipelines, only stop when both client and server shutdown
    t_c2s = threading.Thread(target=client_to_server, args=(sock_client, sock_server, client_ip, p_dict, log))
    t_c2s.setDaemon(True)
    t_s2c = threading.Thread(target=server_to_client, args=(sock_server, sock_client, server_ip, p_dict, log))
    t_s2c.setDaemon(True)

    t_c2s.start()
    t_s2c.start()

    t_c2s.join()
    t_s2c.join()