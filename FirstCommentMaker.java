/*
 * Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.services.samples.youtube.cmdline.data;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.samples.youtube.cmdline.Auth;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentSnippet;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadSnippet;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Print a list of videos matching a search term.
 *
 * @author Jeremy Walker
 */
public class FirstCommentMaker {

    /**
     * Define a global variable that identifies the name of a file that
     * contains the developer's API key.
     */
    private static final long NUMBER_OF_VIDEOS_RETURNED = 1;

    /**
     * Initialize a YouTube object to search for videos on YouTube. Then
     * display the name and thumbnail image of each video in the result set.
     *
     * @param args command line args.
     */
    public static void main(String[] args) {

        List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.force-ssl");

        try {
            // This object is used to make YouTube Data API requests. The last
            // argument is required, but since we don't need anything
            // initialized when the HttpRequest is initialized, we override
            // the interface and provide a no-op function.

            /*
             * Define a global instance of a Youtube object, which will be used
             * to make YouTube Data API requests.
             */
            YouTube youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, new HttpRequestInitializer() {
                public void initialize(HttpRequest request) {
                }
            }).setApplicationName("youtube-cmdline-search-sample").build();

            // Define the API request for retrieving search results.
            YouTube.Search.List search = youtube.search().list("id,snippet");

            // Prompt the user to enter a channel id.
            String channelId = getChannelId();

            // Prompt the user to enter a query term.
            String queryTerm = getSearchTerm();

            // Prompt the user for the comment text.
            // Retrieve the text that the user is commenting.
            String commentText = getCommentText();

            // Set your developer key from the {{ Google Cloud Console }} for
            // non-authenticated requests. See:
            // {{ https://cloud.google.com/console }}
            String apiKey = "AIzaSyCWSMHiTxBrWc2nAZLOBZWVFDoOxJzDee4";
            search.setKey(apiKey);
            search.setChannelId(channelId);
            search.setQ(queryTerm);

            // To increase efficiency, only retrieve the fields that the
            // application uses.
            search.setFields("items(id/kind,id/videoId)");
            search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
            search.setOrder("date");
            search.setType("video");

            // Retrieve the video ID that the user is commenting to.
            // Call the API and print results.
            int numSearches = getNumSearches();
            long searchInterval = (long) getSearchInterval();

            String videoId = null;

            ArrayList<String> seenIDs = new ArrayList<String>();

            SearchListResponse searchResponse = search.execute();
            List<SearchResult> searchResultList = searchResponse.getItems();

            if (searchResultList != null) {
                SearchResult singleVideo = searchResultList.iterator().next();
                ResourceId rId = singleVideo.getId();
                seenIDs.add(rId.getVideoId());
            }

            while (numSearches > 0) {
                while (videoId == null && numSearches > 0) {
                    numSearches--;
                    Thread.sleep(searchInterval*1000); //replaces below, no time spent printing
                    /*
                    System.out.print("Searching for new videos, " + numSearches + " left\n");
                    Thread.sleep(searchInterval * 1000 / 3);
                    System.out.println(".");
                    Thread.sleep(searchInterval * 1000 / 3);
                    System.out.println(".");
                    Thread.sleep(searchInterval * 1000 / 3);
                     */
                    searchResponse = search.execute();
                    searchResultList = searchResponse.getItems();
                    if (searchResultList != null) {
                        videoId = getNewVideoId(searchResultList.iterator(), seenIDs);
                        if(videoId != null) {
                            seenIDs.add(videoId);
                        }
                    }
                }

                if(videoId != null) {
                    // Authorize the request.
                    Credential credential = Auth.authorize(scopes, "commentthreads");

                    // This object is used to make YouTube Data API requests.
                    youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                            .setApplicationName("youtube-cmdline-commentthreads-sample").build();

                    // Insert channel comment by omitting videoId.
                    // Create a comment snippet with text.
                    CommentSnippet commentSnippet = new CommentSnippet();
                    commentSnippet.setTextOriginal(commentText);

                    // Create a top-level comment with snippet.
                    Comment topLevelComment = new Comment();
                    topLevelComment.setSnippet(commentSnippet);

                    // Create a comment thread snippet with channelId and top-level
                    // comment.
                    CommentThreadSnippet commentThreadSnippet = new CommentThreadSnippet();
                    commentThreadSnippet.setChannelId(channelId);
                    commentThreadSnippet.setTopLevelComment(topLevelComment);

                    // Create a comment thread with snippet.
                    CommentThread commentThread = new CommentThread();
                    commentThread.setSnippet(commentThreadSnippet);

                    // Insert video comment
                    commentThreadSnippet.setVideoId(videoId);
                    // Call the YouTube Data API's commentThreads.insert method to
                    // create a comment.
                    CommentThread videoCommentInsertResponse = youtube.commentThreads()
                            .insert("snippet", commentThread).execute();
                    // Print information from the API response.
                    System.out
                            .println("\n================== Created Video Comment ==================\n");
                    CommentSnippet snippet = videoCommentInsertResponse.getSnippet().getTopLevelComment()
                            .getSnippet();
                    System.out.println("  - Author: " + snippet.getAuthorDisplayName());
                    System.out.println("  - Comment: " + snippet.getTextDisplay());
                    System.out
                            .println("\n-------------------------------------------------------------\n");

                    videoId = null;
                }
            }

        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    /*
     * Prints out all results in the Iterator. For each result, print the
     * title, video ID, and thumbnail.
     *
     * @param iteratorSearchResults Iterator of SearchResults to print
     *
     * @param query FirstCommentMaker query (String)
     */

    private static String getNewVideoId(Iterator<SearchResult> iteratorSearchResults, ArrayList<String> seenIDs) {

        if (!iteratorSearchResults.hasNext()) {
            System.out.println(" There aren't any results for your query.");
        }

        while (iteratorSearchResults.hasNext()) {

            SearchResult singleVideo = iteratorSearchResults.next();
            ResourceId rId = singleVideo.getId();

            // Confirm that the result represents a video. Otherwise, the
            // item will not contain a video ID.
            if (rId.getKind().equals("youtube#video")) {
                if(!seenIDs.contains(rId.getVideoId())) {
                    System.out.println("\n-------------------------------------------------------------\n");
                    System.out.println(" Video Id" + rId.getVideoId());
                    System.out.println("\n-------------------------------------------------------------\n");
                    return rId.getVideoId();
                }
            }
        }
        return null;
    }

    /*
     * Prompt the user to enter a search term. Then return the ID.
     */
    private static String getSearchTerm() throws IOException {

        String searchTerm;

        System.out.print("Please enter the search term for the search. This should be the name of the channel: ");
        BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
        searchTerm = bReader.readLine();

        return searchTerm;
    }

    /*
     * Prompt the user to enter a channel ID. Then return the ID.
     */
    private static String getChannelId() throws IOException {

        String channelId;

        System.out.print("Please enter the ID of the channel you want to target: ");
        BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
        channelId = bReader.readLine();

        return channelId;
    }

    /*
     * Prompt the user to enter the text for the comment. Then return the text.
     */
    private static String getCommentText() throws IOException {

        String commentText;

        System.out.print("Please enter the text you wish to comment: ");
        BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
        commentText = bReader.readLine();

        return commentText;
    }

    /*
     * Prompt the user to enter the number of searches to be made.
     */
    private static int getNumSearches() throws IOException {

        int numSearches;

        System.out.print("Please enter the number of searches you wish to make. The api allows 100 total in a day: ");
        BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
        numSearches = Integer.parseInt(bReader.readLine());

        return numSearches;
    }

    /*
     * Prompt the user to enter the number of searches to be made.
     */
    private static int getSearchInterval() throws IOException {

        int searchInterval;

        System.out.print("Please enter the interval to wait between searches (in seconds): ");
        BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
        searchInterval = Integer.parseInt(bReader.readLine());

        return searchInterval;
    }
}