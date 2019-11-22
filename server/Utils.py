import string


class bcolors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'

def toHex(s):
    return ":".join("{:02x}".format(c) for c in s)


def uniprint(s):
    if type(s) is bytearray or type(s) is bytes:
        print(bcolors.OKBLUE, toHex(bytearray(s)), bcolors.ENDC)
    elif (type(s) is str) and not all(c in string.printable for c in s):
        print(bcolors.OKGREEN, toHex(str(s)), bcolors.ENDC)
    else:
        print(str(s))


def unistring(s):
    if type(s) is bytearray or type(s) is bytes:
        return bcolors.OKBLUE + toHex(bytearray(s)) + bcolors.ENDC
    elif (type(s) is str) and not all(c in string.printable for c in s):
        return bcolors.OKGREEN + toHex(str(s)) + bcolors.ENDC
    else:
        return str(s)
