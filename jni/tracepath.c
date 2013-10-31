/*
 * tracepath.c
 *
 *		This program is free software; you can redistribute it and/or
 *		modify it under the terms of the GNU General Public License
 *		as published by the Free Software Foundation; either version
 *		2 of the License, or (at your option) any later version.
 *
 * Authors:	Alexey Kuznetsov, <kuznet@ms2.inr.ac.ru>
 *          Ostap Cherkashin
 *
 * OC: this is a severely modified version of the original tracepath.c.
 * The main purpose is to run it as a library on Android devices. Most of
 * the tracepath functionality was cut off.
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/socket.h>
#include <linux/types.h>
#include <linux/errqueue.h>
#include <errno.h>
#include <string.h>
#include <netdb.h>
#include <netinet/in.h>
#include <resolv.h>
#include <sys/time.h>
#include <sys/uio.h>
#include <arpa/inet.h>

void data_wait(int fd) {
    fd_set fds;
    FD_ZERO(&fds);
    FD_SET(fd, &fds);

    struct timeval tv;
    tv.tv_sec = 1;
    tv.tv_usec = 0;

    select(fd+1, &fds, NULL, NULL, &tv);
}

void tracepath(const char *host, int port, char *path) {
    int fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (fd < 0) {
        perror("socket");
        goto exit;
    }

    struct sockaddr_in target;
	struct hostent *he = gethostbyname(host);
	if (he == NULL) {
        perror("gethostbyname");
		goto exit;
	}
	target.sin_family = AF_INET;
    target.sin_port = port;
	memcpy(&target.sin_addr, he->h_addr, 4);

	int on = 1;
	if (setsockopt(fd, SOL_IP, IP_RECVERR, &on, sizeof(on))) {
        perror("IP_RECVERR");
        goto exit;
	}
	if (setsockopt(fd, SOL_IP, IP_RECVTTL, &on, sizeof(on))) {
        perror("IP_RECVTTL");
        goto exit;
	}

    int ttl = 1;
    for (; ttl < 32; ++ttl) {
        on = ttl;
        if (setsockopt(fd, SOL_IP, IP_TTL, &on, sizeof(on))) {
            perror("IP_TTL");
            goto exit;
        }

        char sendbuf[128];
        memset(sendbuf, 0, sizeof(sendbuf));
        sendto(fd, sendbuf, sizeof(sendbuf), 0, (struct sockaddr*)&target, sizeof(target));

        char cbuf[512];
        struct msghdr msg;
        struct cmsghdr *cmsg;
        struct sock_extended_err *e;
        struct sockaddr_in addr;

        msg.msg_name = (__u8*)&addr;
        msg.msg_namelen = sizeof(addr);
        msg.msg_iov = NULL;
        msg.msg_iovlen = 0;
        msg.msg_flags = 0;
        msg.msg_control = cbuf;
        msg.msg_controllen = sizeof(cbuf);

		data_wait(fd);
        if (recvmsg(fd, &msg, MSG_ERRQUEUE) < 0) {
            if (errno == EAGAIN)
                continue;

            perror("MSG_ERRQUEUE");
            goto exit;
        }

        e = NULL;
        for (cmsg = CMSG_FIRSTHDR(&msg); cmsg; cmsg = CMSG_NXTHDR(&msg, cmsg)) {
            if (cmsg->cmsg_level == SOL_IP) {
                if (cmsg->cmsg_type == IP_RECVERR) {
                    e = (struct sock_extended_err *) CMSG_DATA(cmsg);
                }
            }
        }

        if (e != NULL) {
            if (e->ee_origin == SO_EE_ORIGIN_LOCAL)
                sprintf(path, "%s%d:localhost\n", path, ttl);
            else if (e->ee_origin == SO_EE_ORIGIN_ICMP) {
                char node[128];
                struct sockaddr_in *sin = (struct sockaddr_in*)(e + 1);
                inet_ntop(AF_INET, &sin->sin_addr, node, sizeof(node));

                sprintf(path, "%s%d:%s\n", path, ttl, node);
            }
        }

        if (e->ee_errno == ECONNREFUSED) {
            break;
        }
    }

exit:
    close(fd);
}
