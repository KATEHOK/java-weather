# java-weather

Практическое задание по курсу "Программирование на языке Java"

## Зависимости
- Java 25
- [org.json](https://github.com/stleary/JSON-java) 20260814 (лежит в `lib/`)
- Ключ API Яндекс Погоды в переменной окружения `YANDEX_WEATHER_KEY`

## Запуск
```
export YANDEX_WEATHER_KEY=<ключ>
java -cp lib/json-20260814.jar src/Main.java
```
