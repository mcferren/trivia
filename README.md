celebrity trivia
======
Telnet based celebrity guessing game. 

A server that users can connect to using the Windows telnet client. The server tries to guess 
the celebrity that the player is thinking of. The server does this by asking a sequence of yes/no 
questions until it can narrow down the choices to a celebrity that the server has encountered before. 
At that point, the server guesses the celebrity. If the server is wrong, it asks the user for the name 
of the celebrity as well as a yes/no question to distinguish that new celebrity from the guess the server 
made and adds this new information to its “database.” In other words, the server is continuously learning.
Multiple players can connect simultaneously through multithreading; ie. connect multiple separate telnet 
windows to your server and play the game simultaneously. It Synchronizes/locks the game tree node
you are at so that no one else can execute at your tree node while you are there. In other words, only 
“leaf nodes” in the game tree need to be protected. But note, it does NOT restrict two people who are at 
different places in the game tree. The server saves "database" information to stable storage (a disk file). Every 
time the game tree is modified (new celebrities are added), it saves the file to reflect this change. It uses 
a RandomAccessFile to write out only the changed nodes/records.
