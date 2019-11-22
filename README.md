# Grimaldo
An android client app (and cross platform command line server) to remotely unlock your Linux desktop PC

## How it works

The system is made of an Android app, containing a c++ library/binary acting as client, which performs
a challenge-response (SCRAM-like) authentication against a python script (RemoteScreenUnlocker.py, in the *server* folder)
on a Linux PC. Upon successful authentication, the server issues the command:

```shell
loginctl unlock-session <current-session>
```

This may be useful in a company or, in general, a public environment, in order to avoid having to insert password on every PC unlock.
