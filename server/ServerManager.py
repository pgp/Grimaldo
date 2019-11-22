"""
/**
 * *** LITE-VERSION, DOES NOT USE USER STRING, ONLY RANDOM VALUES - AUTH FOR SINGLE USER ***
 * Created by pgp on 06/12/16
 * Custom SCRAM-like authentication class using custom hashing primitives (currently available: Blake2 and SHA3)
 * Summary:
 * | = byte array or string concatenation operator
 * H = Secure hash function immune to length extension attacks (i.e. can be used both as standard hash
 * and as MAC by prepending the shared secret) - e.g. Blake2 or SHA3
 * KDF = Key derivation function, in our case a iterated hash using the previously defined H
 * sp = client's humanly readable password
 * s = shared secret between client and server;
 *      in our case:
 *      s = KDF(sp)
 *
 * Client storage: s (even sp is fine, it would allow to modify existing passwords, without blindly
 * replacing them)
 * salt_S = server salt (16 byte salt)
 *
 * Server storage: H(salt_S | s) , salt_S
 *
 * Beware: on every socket accept, control has to be passed to ClientAuthenticator
 * Exchanged messages and interaction:
 *
 * 1) Client -> Server:
 *      USER_CHALLENGE (16 byte nonce)
 * 2) Server -> Client: (Server authentication to client)
 *      salt_S
 *      SERVER_CHALLENGE (16 byte nonce)
 *      M = H( H(salt_S | s) | USER_CHALLENGE )
 * 3)
 *   ** Client verifies by recomputing M and comparing with the received value **
 *    Client -> Server: (Client authentication to server)
 *      N = H( H(salt_S | s) | SERVER_CHALLENGE | M )
 * After step 3, server's OK is implicit and client can immediately start sending data chunks;
 * if at any step verification fails, any party (client or server) terminates the connection (RST packet)
 *
 */
"""
import hashlib
from os import urandom

import Sizes
from Utils import unistring


def blake2kdf(data, iterations=Sizes.KDF_ITERATIONS):
    hash_function = hashlib.blake2b(data, digest_size=Sizes.HASH_SIZE)
    output = hash_function.digest()
    for i in range(iterations - 1):
        hash_function = hashlib.blake2b(output, digest_size=Sizes.HASH_SIZE)
        output = hash_function.digest()
    return output

def blake2hash(data):
    return blake2kdf(data,1)


class ServerManager(object):

    def __init__(self, logger, connection, sharedSecret):
        self.logger = logger
        self.connection = connection
        self.user = None
        self.salt_S = urandom(Sizes.SALT_SIZE)
        self.sharedSecret = sharedSecret
        self.saltedHash = blake2hash(sharedSecret + self.salt_S)

    def validate(self, item, expectedTypes, expectedSize = None):
        try:
            if type(item) not in expectedTypes or (expectedSize is not None and len(item) != expectedSize):
                self.logger.error(f'Unexpected type or length: {type(item)} : {len(item)}')
                self.connection.close()
                return False
            return True
        except:
            self.logger.error(f'Unexpected type: {type(item)}')
            self.connection.close()
            return False

    def authenticate(self):
        recv_user_challenge = self.connection.recv(1024)
        if not self.validate(recv_user_challenge, [bytes, bytearray], Sizes.CHALLENGE_SIZE): return
        self.logger.debug('User challenge is '+unistring(recv_user_challenge))

        server_challenge = urandom(Sizes.CHALLENGE_SIZE)

        _M = blake2hash(self.saltedHash + recv_user_challenge)

        self.connection.sendall(bytearray(self.salt_S + server_challenge + _M))

        # wait for client response
        client_proof = self.connection.recv(1024)
        print('Validating client proof...')
        if not self.validate(client_proof, [bytes, bytearray], Sizes.HASH_SIZE): return

        computed_proof = blake2hash(self.saltedHash + server_challenge + _M)
        if client_proof != computed_proof:
            self.logger.error('***client proof differs from computed proof***')
            self.connection.close()  # client verification failed, disconnect
            return
        print('Successfully authenticated')
        return True

    def simulate_client_auth(self, connection):
        user_challenge = urandom(Sizes.CHALLENGE_SIZE)
        connection.sendall(user_challenge)

        salt_serverChallenge_M = connection.recv(1024)
        if not self.validate(salt_serverChallenge_M,
                             [bytes,bytearray],
                             Sizes.SALT_SIZE+Sizes.CHALLENGE_SIZE+Sizes.HASH_SIZE):
            return
        serverSalt = salt_serverChallenge_M[:Sizes.SALT_SIZE]
        serverChallenge = salt_serverChallenge_M[Sizes.SALT_SIZE:Sizes.SALT_SIZE+Sizes.CHALLENGE_SIZE]
        M = salt_serverChallenge_M[Sizes.SALT_SIZE+Sizes.CHALLENGE_SIZE:]

        recomputed_M = blake2hash(blake2hash(self.sharedSecret + serverSalt) + user_challenge)
        if recomputed_M != M:
            connection.close()
            raise Exception("Proofs differ!")
        N = blake2hash(blake2hash(self.sharedSecret + serverSalt) + serverChallenge + M)
        connection.sendall(N)