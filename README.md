# Java TCP Chat Room

A multi-user group chat built on raw Java TCP sockets. The server accepts any
number of clients on a thread pool and broadcasts timestamped messages to
everyone connected. Clients run in the terminal.

## Contents

- [Features](#features)
- [Requirements](#requirements)
- [Building](#building)
- [Running](#running)
- [Commands](#commands)
- [Administrator role](#administrator-role)
- [Connecting from another machine](#connecting-from-another-machine)
- [Notes and limitations](#notes-and-limitations)

## Features

- Multi-client group chat over TCP, one thread per client
- Messages broadcast to all users with a `dd-MM-yyyy HH:mm:ss` timestamp
- Private messaging between users
- Renaming yourself at any time
- An administrator role, granted automatically and reassigned when the current
  admin disconnects

## Requirements

A JDK (Java 8 or later) with `javac` and `java` on your `PATH`. An IDE is
optional — everything below works from a plain terminal.

## Building

From the directory containing the source files:

```bash
javac Host.java Client.java
```

## Running

**1. Start the server.**

```bash
java Host
```

The server listens on port `9999` and logs joins, renames, and kicks to its own
terminal. Leave this window open.

**2. Start a client** in a second terminal:

```bash
java Client
```

You will be prompted to enter an ID. Once you do, you are in the chat.

**3. Add more users** by opening another terminal for each one and running
`java Client` again. Every client needs its own terminal window.

## Commands

Commands are prefixed with `/` and are case-sensitive. Replace anything in
`<angle brackets>` with your own text.

| Command                 | Description                                          |
| ----------------------- | ---------------------------------------------------- |
| `/changeID <ID>`        | Change your display name                             |
| `/pm <ID> <message>`    | Send a private message to one user                   |
| `/info`                 | Show your ID and socket details (IP and port)        |
| `/kick <ID>`            | Disconnect another user — **admin only**             |
| `/quit`                 | Leave the chat and close your client                 |

Anything not starting with `/` is broadcast to everyone.

## Administrator role

The first user to connect is automatically made administrator. Their ID is
prefixed with `Admin `, so a user who enters `bob` will appear as `Admin bob`.

If the administrator disconnects, the role is passed to the next connected user
automatically, and everyone is notified.

Because the prefix becomes part of the ID, commands that target the admin need
the full name — `/pm Admin bob hello`, not `/pm bob hello`.

## Connecting from another machine

The client currently connects to a hardcoded address:

```java
client = new Socket("127.0.0.1", 9999);
```

This means all clients must run on the same machine as the server. To chat
across a network, change `127.0.0.1` to the server's IP address in `Client.java`
and recompile. Make sure port `9999` is open on the server's firewall.

## Notes and limitations

- IDs cannot contain spaces. `/pm` and `/kick` split on the first space, so an ID
  with a space in it cannot be targeted reliably.
- The server address and port are hardcoded rather than read from arguments or a
  config file.
- There is no authentication. Anyone who can reach the port can join, and the
  admin role is granted purely by connection order.
