package org.fengling.anti_addiction;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CodeforcesAPI {

    private static final String USER_INFO_API_URL = "https://codeforces.com/api/user.info?handles=";
    private static final String CONTEST_LIST_API_URL = "https://codeforces.com/api/contest.list";
    private static final String MIRROR_CODEFORCES_URL = "https://mirror.codeforces.com/"; // mirror.codeforces.com 网址

    // --- User Info Functionality ---

    // ... (UserInfo, CodeforcesResponse, getUserInfoJson, parseUserInfoJson 类和方法保持不变)
    public static class UserInfo {
        public int contribution;
        public long lastOnlineTimeSeconds;
        public String organization;
        public int rating;
        public int friendOfCount;
        public String titlePhoto;
        public String rank;
        public String handle;
        public int maxRating;
        public String avatar;
        public long registrationTimeSeconds;
        public String maxRank;

        @Override
        public String toString() { // For debugging
            return "UserInfo{" +
                    "contribution=" + contribution +
                    ", lastOnlineTimeSeconds=" + lastOnlineTimeSeconds +
                    ", organization='" + organization + '\'' +
                    ", rating=" + rating +
                    ", friendOfCount=" + friendOfCount +
                    ", titlePhoto='" + titlePhoto + '\'' +
                    ", rank='" + rank + '\'' +
                    ", handle='" + handle + '\'' +
                    ", maxRating=" + maxRating +
                    ", avatar='" + avatar + '\'' +
                    ", registrationTimeSeconds=" + registrationTimeSeconds +
                    ", maxRank='" + maxRank + '\'' +
                    '}';
        }
    }

    public static class CodeforcesResponse {
        public String status;
        public List<UserInfo> result;

        @Override
        public String toString() { // For debugging
            return "CodeforcesResponse{" +
                    "status='" + status + '\'' +
                    ", result=" + result +
                    '}';
        }
    }

    public static String getUserInfoJson(String handle) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USER_INFO_API_URL + handle))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            Anti_addiction.LOGGER.error("Error fetching Codeforces user data: {}", e.getMessage());
            return null;  // Or handle the error differently
        }
    }

    public static CodeforcesResponse parseUserInfoJson(String json) {
        CodeforcesResponse response = new CodeforcesResponse();
        response.result = new ArrayList<>();

        try {
            Map<String, Object> parsedData = (Map<String, Object>) GenericJsonParser.parse(json);

            response.status = (String) parsedData.get("status");

            List<Map<String, Object>> results = (List<Map<String, Object>>) parsedData.get("result");
            if (results != null) {
                for (Map<String, Object> result : results) {
                    UserInfo userInfo = new UserInfo();
                    userInfo.contribution = ((Number) result.get("contribution")).intValue();
                    userInfo.lastOnlineTimeSeconds = ((Number) result.get("lastOnlineTimeSeconds")).longValue();
                    userInfo.organization = (String) result.get("organization");
                    userInfo.rating = ((Number) result.get("rating")).intValue();
                    userInfo.friendOfCount = ((Number) result.get("friendOfCount")).intValue();
                    userInfo.titlePhoto = (String) result.get("titlePhoto");
                    userInfo.rank = (String) result.get("rank");
                    userInfo.handle = (String) result.get("handle");
                    userInfo.maxRating = ((Number) result.get("maxRating")).intValue();
                    userInfo.avatar = (String) result.get("avatar");
                    userInfo.registrationTimeSeconds = ((Number) result.get("registrationTimeSeconds")).longValue();
                    userInfo.maxRank = (String) result.get("maxRank");

                    response.result.add(userInfo);
                }
            }

            return response;

        } catch (IOException e) {
            Anti_addiction.LOGGER.error("Error parsing UserInfo JSON: {}", e.getMessage());
            return null;
        }
    }


    // --- Contest List Functionality ---
    // ... (ContestInfo, ContestListResponse, getRecentContestsJson, parseContestListJson 类和方法保持不变)

    public static class ContestInfo {
        public int id;
        public String name;
        public String type;
        public String phase;
        public long startTimeSeconds;
        public int durationSeconds;
        public String preparedBy;
        public String websiteUrl;
        public String description;
        public String difficulty;
        public String icpcRegion;
        public String country;
        public String city;
        public String season;

        @Override
        public String toString() {
            return "ContestInfo{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", phase='" + phase + '\'' +
                    ", startTimeSeconds=" + startTimeSeconds +
                    ", durationSeconds=" + durationSeconds +
                    ", preparedBy='" + preparedBy + '\'' +
                    ", websiteUrl='" + websiteUrl + '\'' +
                    ", description='" + description + '\'' +
                    ", difficulty='" + difficulty + '\'' +
                    ", icpcRegion='" + icpcRegion + '\'' +
                    ", country='" + country + '\'' +
                    ", city='" + city + '\'' +
                    ", season='" + season + '\'' +
                    '}';
        }
    }

    public static class ContestListResponse {
        public String status;
        public List<ContestInfo> result;

        @Override
        public String toString() {
            return "ContestListResponse{" +
                    "status='" + status + '\'' +
                    ", result=" + result +
                    '}';
        }
    }


    public static String getRecentContestsJson() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CONTEST_LIST_API_URL))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            Anti_addiction.LOGGER.error("Error fetching Codeforces contest list: {}", e.getMessage());
            return null;
        }
    }

    public static ContestListResponse parseContestListJson(String json) {
        ContestListResponse response = new ContestListResponse();
        response.result = new ArrayList<>();

        try {
            Map<String, Object> parsedData = (Map<String, Object>) GenericJsonParser.parse(json);
            response.status = (String) parsedData.get("status");
            List<Map<String, Object>> results = (List<Map<String, Object>>) parsedData.get("result");

            if (results != null) {
                for (Map<String, Object> contestData : results) {
                    ContestInfo contestInfo = new ContestInfo();
                    contestInfo.id = ((Number) contestData.get("id")).intValue();
                    contestInfo.name = (String) contestData.get("name");
                    contestInfo.type = (String) contestData.get("type");
                    contestInfo.phase = (String) contestData.get("phase");
                    contestInfo.startTimeSeconds = ((Number) contestData.get("startTimeSeconds")).longValue();
                    contestInfo.durationSeconds = ((Number) contestData.get("durationSeconds")).intValue();
                    contestInfo.preparedBy = (String) contestData.get("preparedBy");
                    contestInfo.websiteUrl = (String) contestData.get("websiteUrl");
                    contestInfo.description = (String) contestData.get("description");
                    contestInfo.difficulty = (String) contestData.get("difficulty");
                    contestInfo.icpcRegion = (String) contestData.get("icpcRegion");
                    contestInfo.country = (String) contestData.get("country");
                    contestInfo.city = (String) contestData.get("city");
                    contestInfo.season = (String) contestData.get("season");

                    response.result.add(contestInfo);
                }
            }
            return response;

        } catch (IOException e) {
            Anti_addiction.LOGGER.error("Error parsing ContestList JSON: {}", e.getMessage());
            return null;
        }
    }


    // --- Mirror Codeforces Page Source ---

    /**
     * 获取 mirror.codeforces.com 网页的 HTML 源代码.
     *
     * @return 网页源代码字符串，如果获取失败则返回 null.
     */
    public static String getMirrorCodeforcesPageSource() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MIRROR_CODEFORCES_URL))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            Anti_addiction.LOGGER.error("Error fetching mirror.codeforces.com page source: {}", e.getMessage());
            return null;
        }
    }


    public static void main(String[] args) {
        Anti_addiction.LOGGER.info("--- User Info Test ---");
        String userInfoJson = getUserInfoJson("Empty_Dust");
        if (userInfoJson != null) {
            Anti_addiction.LOGGER.info(userInfoJson);
            CodeforcesResponse userInfoResponse = parseUserInfoJson(userInfoJson);

            if (userInfoResponse != null) {
                Anti_addiction.LOGGER.info("Status: {}", userInfoResponse.status);

                if (userInfoResponse.result != null) {
                    for (UserInfo user : userInfoResponse.result) {
                        Anti_addiction.LOGGER.info("Handle: {}", user.handle);
                        Anti_addiction.LOGGER.info("Rating: {}", user.rating);
                        Anti_addiction.LOGGER.info("maxRating: {}", user.maxRating);
                        Anti_addiction.LOGGER.info("registrationTimeSeconds: {}", user.registrationTimeSeconds);
                    }
                }
            }
        }


        Anti_addiction.LOGGER.info("--- Recent Contest List ---");
        String contestListJson = getRecentContestsJson();
        if (contestListJson != null) {
            ContestListResponse contestListResponse = parseContestListJson(contestListJson);

            if (contestListResponse != null) {
                Anti_addiction.LOGGER.info("Status: {}", contestListResponse.status);
                if (contestListResponse.result != null) {
                    Anti_addiction.LOGGER.info("Recent and Upcoming Contests (within 3 days or not started yet):");

                    long currentTimeSeconds = System.currentTimeMillis() / 1000;
                    long threeDaysAgoSeconds = currentTimeSeconds - (3 * 24 * 60 * 60);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());


                    for (ContestInfo contest : contestListResponse.result) {
                        if (contest.phase.equals("BEFORE") || (contest.startTimeSeconds >= threeDaysAgoSeconds && !contest.phase.equals("BEFORE"))) {
                            LocalDateTime startTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(contest.startTimeSeconds), ZoneId.systemDefault());
                            String formattedStartTime = formatter.format(startTime);

                            String duration = String.format("%d hours %d minutes", contest.durationSeconds / 3600, (contest.durationSeconds % 3600) / 60);

                            Anti_addiction.LOGGER.info("----------------------------------");
                            Anti_addiction.LOGGER.info("Name: {}", contest.name);
                            Anti_addiction.LOGGER.info("Type: {}", contest.type);
                            Anti_addiction.LOGGER.info("Start Time: {}", formattedStartTime);
                            Anti_addiction.LOGGER.info("Duration: {}", duration);
                        }
                    }
                    Anti_addiction.LOGGER.info("----------------------------------");
                }
            }
        }

        Anti_addiction.LOGGER.info("--- Mirror Codeforces Page Source Test ---"); // 添加 Mirror Codeforces 网页源代码测试
        String mirrorPageSource = getMirrorCodeforcesPageSource();
        if (mirrorPageSource != null) {
            Anti_addiction.LOGGER.info("Mirror Codeforces Page Source (First 500 characters):\n{}", mirrorPageSource.substring(0, Math.min(500, mirrorPageSource.length()))); // 打印前 500 个字符
        } else {
            Anti_addiction.LOGGER.error("Failed to fetch mirror.codeforces.com page source.");
        }
    }
}