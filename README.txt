Welcome to the FirstCommentMaker!


This script works by using the Youtube Data Analytics v3 API to preform searches and create comments. It finds the last posted video of a given channel each time a search is performed, and if that last video ever changes (meaning a new one was posted) it creates a comment specified by you on the new video using your account. 

Due to query limits put in place by YouTube, only ~100 actions are permitted through the API per 24 hours. You should take this into account when defining your search intervals. If you want the program to be searching all day, your search interval needs to be at least ~15 minutes to not exceed the quota, but if you know a video will be published within a 30 minute range, you can have a 15-20 second interval.


1. To use, download the file to a local directory.

2. Generate a Google API key with the following link: https://console.cloud.google.com/apis/api/youtube.googleapis.com/credentials and add your API key to the program on line 80

3. Mark as executable with "chmod +x [your-file-path]"

4. Run the program with ./FirstCommentMaker. The program will then ask for various input needed to preform the search. You will also be asked to sign into your google account with OAuth. 

Enjoy!
