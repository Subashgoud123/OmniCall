class LanguageOption {
  LanguageOption({required this.code, required this.englishName, required this.nativeName, required this.enabled});
  final String code;
  final String englishName;
  final String nativeName;
  final bool enabled;

  factory LanguageOption.fromJson(Map<String, dynamic> json) => LanguageOption(
        code: json['code'] as String,
        englishName: json['englishName'] as String,
        nativeName: json['nativeName'] as String,
        enabled: json['enabled'] as bool? ?? false,
      );
}

class UserProfile {
  UserProfile({
    required this.id,
    required this.displayName,
    required this.phoneNumber,
    required this.preferredLanguage,
    required this.translationConsent,
    this.profileImageUrl,
    this.preferredVoice,
    this.voiceGender = 'FEMALE',
    this.subtitleEnabled = true,
    this.originalAudioEnabled = false,
    this.translationSpeed = 1,
    this.notificationsEnabled = true,
    this.translationHistoryEnabled = false,
  });

  final String id;
  final String displayName;
  final String phoneNumber;
  final String preferredLanguage;
  final bool translationConsent;
  final String? profileImageUrl;
  final String? preferredVoice;
  final String voiceGender;
  final bool subtitleEnabled;
  final bool originalAudioEnabled;
  final double translationSpeed;
  final bool notificationsEnabled;
  final bool translationHistoryEnabled;

  factory UserProfile.fromJson(Map<String, dynamic> json) => UserProfile(
        id: json['id'].toString(),
        displayName: json['displayName'] as String? ?? '',
        phoneNumber: json['phoneNumber'] as String? ?? '',
        preferredLanguage: json['preferredLanguage'] as String? ?? 'hi-IN',
        translationConsent: json['translationConsent'] as bool? ?? false,
        profileImageUrl: json['profileImageUrl'] as String?,
        preferredVoice: json['preferredVoice'] as String?,
        voiceGender: json['voiceGender'] as String? ?? 'FEMALE',
        subtitleEnabled: json['subtitleEnabled'] as bool? ?? true,
        originalAudioEnabled: json['originalAudioEnabled'] as bool? ?? false,
        translationSpeed: (json['translationSpeed'] as num?)?.toDouble() ?? 1,
        notificationsEnabled: json['notificationsEnabled'] as bool? ?? true,
        translationHistoryEnabled: json['translationHistoryEnabled'] as bool? ?? false,
      );
}

class PublicUser {
  PublicUser({required this.id, required this.displayName, required this.preferredLanguage, this.profileImageUrl});
  final String id;
  final String displayName;
  final String preferredLanguage;
  final String? profileImageUrl;

  factory PublicUser.fromJson(Map<String, dynamic> json) => PublicUser(
        id: json['id'].toString(),
        displayName: json['displayName'] as String? ?? '',
        preferredLanguage: json['preferredLanguage'] as String? ?? '',
        profileImageUrl: json['profileImageUrl'] as String?,
      );
}

class ContactEntry {
  ContactEntry({required this.id, required this.user, required this.status});
  final String id;
  final PublicUser user;
  final String status;

  factory ContactEntry.fromJson(Map<String, dynamic> json) => ContactEntry(
        id: json['id'].toString(),
        user: PublicUser.fromJson(json['user'] as Map<String, dynamic>),
        status: json['status'] as String? ?? 'INVITED',
      );
}

class CallSession {
  CallSession({
    required this.id,
    required this.callerId,
    required this.receiverId,
    required this.callerName,
    required this.receiverName,
    required this.callerLanguage,
    required this.receiverLanguage,
    required this.status,
    this.callerImageUrl,
    this.receiverImageUrl,
    this.durationSeconds,
  });

  final String id;
  final String callerId;
  final String receiverId;
  final String callerName;
  final String receiverName;
  final String? callerImageUrl;
  final String? receiverImageUrl;
  final String callerLanguage;
  final String receiverLanguage;
  final String status;
  final int? durationSeconds;

  factory CallSession.fromJson(Map<String, dynamic> json) => CallSession(
        id: json['id'].toString(),
        callerId: json['callerId'].toString(),
        receiverId: json['receiverId'].toString(),
        callerName: json['callerName'] as String? ?? '',
        receiverName: json['receiverName'] as String? ?? '',
        callerImageUrl: json['callerImageUrl'] as String?,
        receiverImageUrl: json['receiverImageUrl'] as String?,
        callerLanguage: json['callerLanguage'] as String? ?? '',
        receiverLanguage: json['receiverLanguage'] as String? ?? '',
        status: json['status'] as String? ?? '',
        durationSeconds: json['durationSeconds'] as int?,
      );

  String otherName(String userId) => userId == callerId ? receiverName : callerName;
  String otherLanguage(String userId) => userId == callerId ? receiverLanguage : callerLanguage;
}

class SubtitleCue {
  SubtitleCue({
    required this.speakerId,
    required this.sourceLanguage,
    required this.targetLanguage,
    required this.originalText,
    required this.translatedText,
    required this.status,
    required this.partial,
  });

  final String speakerId;
  final String sourceLanguage;
  final String targetLanguage;
  final String originalText;
  final String translatedText;
  final String status;
  final bool partial;

  factory SubtitleCue.fromJson(Map<String, dynamic> json) => SubtitleCue(
        speakerId: json['speakerId']?.toString() ?? '',
        sourceLanguage: json['sourceLanguage'] as String? ?? '',
        targetLanguage: json['targetLanguage'] as String? ?? '',
        originalText: json['originalText'] as String? ?? '',
        translatedText: json['translatedText'] as String? ?? '',
        status: json['status'] as String? ?? '',
        partial: json['partial'] as bool? ?? false,
      );
}

class CallCredentials {
  CallCredentials({required this.url, required this.token, required this.roomName});
  final String url;
  final String token;
  final String roomName;

  factory CallCredentials.fromJson(Map<String, dynamic> json) => CallCredentials(
        url: json['url'] as String,
        token: json['token'] as String,
        roomName: json['roomName'] as String,
      );
}
