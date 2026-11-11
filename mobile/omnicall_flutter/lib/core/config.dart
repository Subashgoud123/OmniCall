class AppConfig {
  static const apiBase = String.fromEnvironment('API_BASE', defaultValue: 'http://10.0.2.2:8080');
  static const wsBase = String.fromEnvironment('WS_BASE', defaultValue: 'ws://10.0.2.2:8080/ws');
}
