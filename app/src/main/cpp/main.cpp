#include <iostream>
#include <cstdio>
#include <vector>
#include "blake2.h"
#include "PosixDescriptor.h"

constexpr uint16_t HASH_SIZE = 32;
constexpr uint16_t CHALLENGE_SIZE = 16;
constexpr uint16_t SALT_SIZE = 16;

void printHex(uint8_t* buf, int bufLen) {
    int i;
    for (i = 0; i < bufLen; i++) {
        if (i > 0) printf(":");
        printf("%02X", buf[i]);
    }
    printf("\n");
}

std::vector<uint8_t> blake2hash(std::vector<uint8_t>& input) {
    std::vector<uint8_t> out(HASH_SIZE,0);
    blake2b_state S;
    blake2b_init(&S, HASH_SIZE);
    blake2b_update(&S, &input[0], input.size());
    blake2b_final(&S, &out[0], HASH_SIZE);
    return out;
}

std::vector<uint8_t> blake2kdf(const std::string& input, uint32_t iterations = 1048576) {
    std::vector<uint8_t> out(HASH_SIZE,0);
    blake2b_state S;
    blake2b_init(&S, HASH_SIZE);
    blake2b_update(&S, &input[0], input.size());
    blake2b_final(&S, &out[0], HASH_SIZE);
    for(uint32_t i=0; i<iterations-1; i++) {
        blake2b_init(&S, HASH_SIZE);
        blake2b_update(&S, &out[0], HASH_SIZE);
        blake2b_final(&S, &out[0], HASH_SIZE);
    }
    return out;
}

int connect_with_timeout(int& sock_fd, struct addrinfo* p, unsigned timeout_seconds = 5) {
    int res;
    //~ struct sockaddr_in addr;
    long arg;
    fd_set myset;
    struct timeval tv{};
    int valopt;
    socklen_t lon;

    // Create socket
    // sock_fd = socket(AF_INET, SOCK_STREAM, 0);
    sock_fd = socket(p->ai_family, p->ai_socktype, p->ai_protocol);

    if (sock_fd < 0) {
        PRINTUNIFIEDERROR("Error creating socket (%d %s)\n", errno, strerror(errno));
        return -1;
    }

    // Set non-blocking
    if( (arg = fcntl(sock_fd, F_GETFL, nullptr)) < 0) {
        PRINTUNIFIEDERROR("Error fcntl(..., F_GETFL) (%s)\n", strerror(errno));
        return -2;
    }
    arg |= O_NONBLOCK;
    if( fcntl(sock_fd, F_SETFL, arg) < 0) {
        PRINTUNIFIEDERROR("Error fcntl(..., F_SETFL) (%s)\n", strerror(errno));
        return -3;
    }
    // Trying to connect with timeout
    // res = connect(sock_fd, (struct sockaddr *)&addr, sizeof(addr));
    res = connect(sock_fd, p->ai_addr, p->ai_addrlen);
    if (res < 0) {
        if (errno == EINPROGRESS) {
            PRINTUNIFIEDERROR("EINPROGRESS in connect() - selecting\n");
            for(;;) {
                tv.tv_sec = timeout_seconds;
                tv.tv_usec = 0;
                FD_ZERO(&myset);
                FD_SET(sock_fd, &myset);
                res = select(sock_fd+1, nullptr, &myset, nullptr, &tv);
                if (res < 0 && errno != EINTR) {
                    PRINTUNIFIEDERROR("Error connecting %d - %s\n", errno, strerror(errno));
                    return -4;
                }
                else if (res > 0) {
                    // Socket selected for write
                    lon = sizeof(int);
                    if (getsockopt(sock_fd, SOL_SOCKET, SO_ERROR, (void*)(&valopt), &lon) < 0) {
                        PRINTUNIFIEDERROR("Error in getsockopt() %d - %s\n", errno, strerror(errno));
                        return -5;
                    }
                    // Check the value returned...
                    if (valopt) {
                        PRINTUNIFIEDERROR("Error in delayed connection() %d - %s\n", valopt, strerror(valopt));
                        return -6;
                    }
                    break;
                }
                else {
                    PRINTUNIFIEDERROR("Timeout in select() - Cancelling!\n");
                    return -7;
                }
            }
        }
        else {
            PRINTUNIFIEDERROR("Error connecting %d - %s\n", errno, strerror(errno));
            return -8;
        }
    }
    // Set to blocking mode again...
    if( (arg = fcntl(sock_fd, F_GETFL, nullptr)) < 0) {
        PRINTUNIFIEDERROR("Error fcntl(..., F_GETFL) (%s)\n", strerror(errno));
        return -9;
    }
    arg &= (~O_NONBLOCK);
    if( fcntl(sock_fd, F_SETFL, arg) < 0) {
        PRINTUNIFIEDERROR("Error fcntl(..., F_SETFL) (%s)\n", strerror(errno));
        return -10;
    }
    return 0; // ok, at this point the socket is connected and again in blocking mode
}


int get_connected_descriptor(const std::string& domainOnly, int port) {
    int remoteCl;
    if ((remoteCl = socket(AF_INET, SOCK_STREAM, 0)) == -1) {
        perror("socket");
        _Exit(1);
    }

    struct addrinfo hints, *servinfo, *p;
    int rv;

    PRINTUNIFIED("Populating hints...\n");
    memset(&hints, 0, sizeof hints);
    hints.ai_family = AF_INET; // use AF_INET to force IPv4, AF_INET6 to force IPv6, AF_UNSPEC to allow both
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    std::string port_s = std::to_string(port);

    PRINTUNIFIED("Invoking getaddrinfo for %s\n",domainOnly.c_str());
    if ((rv = getaddrinfo(domainOnly.c_str(), port_s.c_str(), &hints, &servinfo)) != 0) {
        PRINTUNIFIEDERROR("getaddrinfo error: %s\n", gai_strerror(rv));
        return -1;
    }

    PRINTUNIFIED("Looping through getaddrinfo results...\n");
    // loop through all the results and connect to the first we can
    for(p = servinfo; p != nullptr; p = p->ai_next) {
        PRINTUNIFIED("getaddrinfo item\n");

        // NEW, with timeout
        rv = connect_with_timeout(remoteCl, p);
        if (rv == 0) break;
        else {
            PRINTUNIFIEDERROR("Timeout or connection error %d\n",rv);
            close(remoteCl);
        }
    }
    PRINTUNIFIED("getaddrinfo end results\n");

    if (p == nullptr) {
        freeaddrinfo(servinfo);
        PRINTUNIFIED("Could not create socket or connect\n");
        errno = 0x323232;
        return -1;
    }
    PRINTUNIFIED("freeaddrinfo...\n");
    freeaddrinfo(servinfo);
    PRINTUNIFIED("Client connected to server %s, port %d\n",domainOnly.c_str(),port);

    return remoteCl;
}

void authenticate(const std::string& passphrase, const std::string& domainOnly, int port) {
    int remoteCl = get_connected_descriptor(domainOnly, port);
    int urandom = open("/dev/urandom", O_RDONLY);
    PosixDescriptor cl(remoteCl);
    PosixDescriptor ur(urandom);

    std::vector<uint8_t> user_challenge(CHALLENGE_SIZE,0);
    ur.readAllOrExit(&user_challenge[0],CHALLENGE_SIZE);
    cl.writeAllOrExit(&user_challenge[0],CHALLENGE_SIZE);

    std::vector<uint8_t> salt_serverChallenge_M(SALT_SIZE + CHALLENGE_SIZE + HASH_SIZE,0);
    cl.readAllOrExit(&salt_serverChallenge_M[0], SALT_SIZE + CHALLENGE_SIZE + HASH_SIZE);

    std::vector<uint8_t> serverSalt(&salt_serverChallenge_M[0], &salt_serverChallenge_M[SALT_SIZE]);
    std::vector<uint8_t> serverChallenge(&salt_serverChallenge_M[SALT_SIZE], &salt_serverChallenge_M[SALT_SIZE+CHALLENGE_SIZE]);
    std::vector<uint8_t> M(&salt_serverChallenge_M[SALT_SIZE+CHALLENGE_SIZE], &salt_serverChallenge_M[SALT_SIZE+CHALLENGE_SIZE+HASH_SIZE]);


    auto&& sharedSecret = blake2kdf(passphrase);
    sharedSecret.insert(sharedSecret.end(), serverSalt.begin(), serverSalt.end());
    auto&& intermediateHash = blake2hash(sharedSecret);
    auto ii = intermediateHash;
    ii.insert(ii.end(), user_challenge.begin(), user_challenge.end());
    auto&& recomputed_M = blake2hash(ii);

    if(recomputed_M != M) {
        cl.close();
        throw std::runtime_error("Proofs differ!");
    }

    intermediateHash.insert(intermediateHash.end(), serverChallenge.begin(), serverChallenge.end());
    intermediateHash.insert(intermediateHash.end(), M.begin(), M.end());
    auto&& N = blake2hash(intermediateHash);
    cl.writeAllOrExit(&N[0], N.size());
}

int main(int argc, char* argv[]) {

    if(argc < 4) {
        PRINTUNIFIED("Usage: %s <passphrase> <IP> <port>\n", argv[0]);
        return 0;
    }
    std::string passphrase(argv[1]);
    std::string ip(argv[2]);
    std::string port(argv[3]);
    authenticate(passphrase, ip, std::stoi(port));

    return 0;
}