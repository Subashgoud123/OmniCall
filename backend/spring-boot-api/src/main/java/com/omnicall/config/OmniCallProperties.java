package com.omnicall.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "omnicall")
public class OmniCallProperties {

    private final Jwt jwt = new Jwt();
    private final Otp otp = new Otp();
    private final Sms sms = new Sms();
    private final LiveKit livekit = new LiveKit();
    private final Ai ai = new Ai();
    private final Usage usage = new Usage();
    private final Media media = new Media();
    private final Fcm fcm = new Fcm();
    private final Privacy privacy = new Privacy();
    private String internalToken;

    public Jwt getJwt() {
        return jwt;
    }

    public Otp getOtp() {
        return otp;
    }

    public Sms getSms() {
        return sms;
    }

    public LiveKit getLivekit() {
        return livekit;
    }

    public Ai getAi() {
        return ai;
    }

    public Usage getUsage() {
        return usage;
    }

    public Media getMedia() {
        return media;
    }

    public Fcm getFcm() {
        return fcm;
    }

    public Privacy getPrivacy() {
        return privacy;
    }

    public String getInternalToken() {
        return internalToken;
    }

    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }

    public static class Jwt {
        private String secret;
        private Duration accessTtl = Duration.ofMinutes(15);
        private Duration refreshTtl = Duration.ofDays(30);

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Duration getAccessTtl() {
            return accessTtl;
        }

        public void setAccessTtl(Duration accessTtl) {
            this.accessTtl = accessTtl;
        }

        public Duration getRefreshTtl() {
            return refreshTtl;
        }

        public void setRefreshTtl(Duration refreshTtl) {
            this.refreshTtl = refreshTtl;
        }
    }

    public static class Otp {
        private Duration ttl = Duration.ofMinutes(5);
        private int length = 6;
        private int maxAttempts = 5;

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }

    public static class Sms {
        private String provider = "log";
        private String twilioAccountSid = "";
        private String twilioAuthToken = "";
        private String twilioFromNumber = "";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getTwilioAccountSid() {
            return twilioAccountSid;
        }

        public void setTwilioAccountSid(String twilioAccountSid) {
            this.twilioAccountSid = twilioAccountSid;
        }

        public String getTwilioAuthToken() {
            return twilioAuthToken;
        }

        public void setTwilioAuthToken(String twilioAuthToken) {
            this.twilioAuthToken = twilioAuthToken;
        }

        public String getTwilioFromNumber() {
            return twilioFromNumber;
        }

        public void setTwilioFromNumber(String twilioFromNumber) {
            this.twilioFromNumber = twilioFromNumber;
        }
    }

    public static class LiveKit {
        private String host;
        private String url;
        private String apiKey;
        private String apiSecret;
        private Duration tokenTtl = Duration.ofMinutes(10);
        private String agentName = "omnicall-translator";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiSecret() {
            return apiSecret;
        }

        public void setApiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
        }

        public Duration getTokenTtl() {
            return tokenTtl;
        }

        public void setTokenTtl(Duration tokenTtl) {
            this.tokenTtl = tokenTtl;
        }

        public String getAgentName() {
            return agentName;
        }

        public void setAgentName(String agentName) {
            this.agentName = agentName;
        }
    }

    public static class Ai {
        private String baseUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Usage {
        private long dailyCallSeconds = 3600;

        public long getDailyCallSeconds() {
            return dailyCallSeconds;
        }

        public void setDailyCallSeconds(long dailyCallSeconds) {
            this.dailyCallSeconds = dailyCallSeconds;
        }
    }

    public static class Media {
        private String directory = "./data/avatars";

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }
    }

    public static class Fcm {
        private String credentialsPath = "";
        private String projectId = "";

        public String getCredentialsPath() {
            return credentialsPath;
        }

        public void setCredentialsPath(String credentialsPath) {
            this.credentialsPath = credentialsPath;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }
    }

    public static class Privacy {
        private String policyVersion = "2026-11-11";
        private int translationRetentionDays = 30;
        private Duration ringTimeout = Duration.ofSeconds(45);

        public String getPolicyVersion() {
            return policyVersion;
        }

        public void setPolicyVersion(String policyVersion) {
            this.policyVersion = policyVersion;
        }

        public int getTranslationRetentionDays() {
            return translationRetentionDays;
        }

        public void setTranslationRetentionDays(int translationRetentionDays) {
            this.translationRetentionDays = translationRetentionDays;
        }

        public Duration getRingTimeout() {
            return ringTimeout;
        }

        public void setRingTimeout(Duration ringTimeout) {
            this.ringTimeout = ringTimeout;
        }
    }
}
