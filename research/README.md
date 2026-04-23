# Research: JetBrains Background Plugin Theory

Дата сборки: 2026-04-23

## Что уже скачано локально

JetBrains:
- `research/articles/jetbrains/intellij-platform.html`
- `research/articles/jetbrains/webstorm.html`
- `research/articles/jetbrains/rider.html`
- `research/articles/jetbrains/themes-getting-started.html`
- `research/articles/jetbrains/themes-extras.html`
- `research/articles/jetbrains/action-system.html`
- `research/articles/jetbrains/idea-background-image.html`

Plugins / examples:
- `research/articles/plugins/intellij-platform-plugin-template.html`
- `research/articles/plugins/intellij-wallpaper.html`
- `research/articles/plugins/intellij-media-player.html`

Windows:
- `research/articles/windows/idesktopwallpaper.html`
- `research/articles/windows/idesktopwallpaper-getwallpaper.html`
- `research/articles/windows/systemparametersinfow.html`

Wallpaper Engine:
- `research/articles/wallpaper-engine/web-overview.html`
- `research/articles/wallpaper-engine/cli.html`
- `research/articles/wallpaper-engine/editingwallpapers.html`

Примечания:
- Страница JetBrains Support про background image для specific editor отдала `403` при `curl`, поэтому выводы по ней сохранены ниже по результатам web-чтения.
- Страница Wallpaper Engine `export.html` была доступна через web-поиск, но не скачалась отдельным `curl`; ключевой вывод ниже сохранен.

## Ключевые выводы

### 1. Для статичной картинки у JetBrains уже есть официальный и поддерживаемый путь

Самый безопасный вариант для плагина:
- сделать theme plugin;
- использовать `background` и `emptyFrameBackground` в theme JSON;
- положить картинку в resources плагина.

Источник:
- JetBrains SDK `Themes - Editor Schemes and Background Images`
- JetBrains SDK `Getting Started`

Что это дает:
- поддерживаемый способ;
- работает во всех IDE на IntelliJ Platform, в том числе в `WebStorm`;
- для `Rider` это тоже применимо на UI-уровне, потому что фон рисуется фронтендом IntelliJ Platform.

Ограничение:
- это статичный background, а не видеопоток и не внешний рендер.

### 2. Для Rider нет отдельного "background API", но есть важная архитектурная особенность

По официальной странице `Rider Plugin Development`:
- `Rider` использует IntelliJ Platform для UI;
- языковые .NET-фичи идут через ReSharper backend;
- если задача чисто UI-шная, например background, tool window, action, настройки, то можно работать на frontend-части без ReSharper backend.

Практический вывод:
- background plugin лучше сначала проектировать как обычный IntelliJ Platform plugin;
- Rider-specific backend нужен только если логика завязана на C# / ReSharper domain model, а не на UI.

### 3. У IDE уже есть встроенный background image

Официальная help-страница IntelliJ IDEA 2026.1 подтверждает:
- пользователь может задать background image через Settings;
- можно вызвать действие `Set Background Image`;
- есть режимы для editor/tools и empty frame;
- поддерживаются local path и URL.

Практический вывод:
- если цель "повторить статичную картинку" с рабочего стола Windows, то плагин может просто обновлять встроенный background image;
- если цель "своя статичная картинка из плагина", проще всего использовать theme plugin;
- если цель "живой" background, встроенной публичной модели недостаточно.

### 4. Фон для конкретного editor-а официально не обещан

По JetBrains Support thread `Changing the background color/image of a specific editor`:
- specific editor background image, по ответу JetBrains, "AFAIU this is not possible";
- если картинка лежит внутри плагина, JetBrains рекомендуют сначала извлечь ее в config directory, а уже потом указывать путь.

Практический вывод:
- target "фон только у одного editor tab" не стоит брать как базовый сценарий;
- реальный target лучше формулировать как global IDE background / editor area background.

### 5. Старые плагины показывают два разных направления

#### `cocuh/intellij-wallpaper`

Что видно из README:
- репозиторий архивирован;
- сам автор пишет `DEPRECATED: use official wallpaper feature`;
- в 2016.2 плагин уже работал плохо.

Вывод:
- это полезный исторический пример, но не база для современной реализации.

#### `wuyr/intellij-media-player`

Что видно из README:
- плагин делает видео как background для JetBrains IDE;
- автор прямо опирался на `idea.background.editor`;
- проект использует внутренние механизмы отрисовки и оптимизацию repaint;
- последний релиз на GitHub: `2021-08-31`.

Вывод:
- это хороший proof of concept для "анимированный background в IDE возможен";
- но это не официальный API и есть риск поломок на новых IDE версиях;
- как источник архитектурных идей проект полезен, как зависимость/эталон production-качества в 2026 году - рискованный.

### 6. Для получения текущих обоев Windows есть официальный API

#### Предпочтительный вариант: `IDesktopWallpaper`

По Microsoft Learn:
- `IDesktopWallpaper` умеет работать с несколькими мониторами;
- `GetWallpaper(monitorID)` возвращает путь к текущему wallpaper image;
- если вызвать с `NULL` и на мониторах разные картинки или идет slideshow, можно получить `S_FALSE` и пустую строку.

Практический вывод:
- для Windows 8+ это лучший официальный API, если нужно повторить именно системную wallpaper-картинку;
- для multi-monitor логики он лучше, чем старый `SystemParametersInfo`.

#### Простой fallback: `SystemParametersInfo(SPI_GETDESKWALLPAPER)`

По Microsoft Learn:
- возвращает полный путь к bitmap file для desktop wallpaper;
- строка не длиннее `MAX_PATH`;
- если wallpaper нет, строка пустая.

Практический вывод:
- можно использовать как простой fallback;
- но это хуже для сложных сценариев с multi-monitor и slideshow.

### 7. Эти Windows API не дают "итоговую анимацию" Wallpaper Engine

Важно:
- `IDesktopWallpaper` и `SPI_GETDESKWALLPAPER` работают с состоянием desktop wallpaper в Windows;
- они дают путь к изображению / wallpaper state;
- они не предоставляют поток финально отрендеренных кадров стороннего wallpaper-движка.

Практический вывод:
- для Wallpaper Engine этого недостаточно, если нужна именно живая анимация, а не статичная картинка.

### 8. По официальной документации Wallpaper Engine публичного API для "забрать финальный видеопоток" не видно

Что найдено в официальных материалах:
- `Web Wallpaper Reference Guide` описывает только создание wallpapers внутри Wallpaper Engine на HTML/CSS/JS;
- `Command Line Controls` умеет открывать wallpaper и даже `playInWindow`;
- `Editing Downloaded Wallpapers` говорит, что `web wallpapers` можно редактировать как HTML/JS, а `application wallpapers` обычно нельзя;
- страница `Exporting wallpapers as GIF / video` прямо говорит, что wallpapers нельзя просто экспортировать как видео/GIF, потому что они больше похожи на "levels in a game", и предлагает только запись через сторонние инструменты.

Практический вывод:
- официально поддерживается управление Wallpaper Engine и разработка wallpaper-контента;
- официального "embed this live rendered wallpaper into another app and stream frames" API в найденной документации нет;
- следовательно, интеграция в IDE на уровне "покажи мне итоговый анимированный output Wallpaper Engine" потребует обходного пути.

## Реалистичные технические варианты

### Вариант A. Повторять статичную картинку Windows wallpaper

Схема:
- получить путь к текущим обоям через `IDesktopWallpaper`;
- скопировать или использовать этот файл;
- применить его как background в IDE.

Плюсы:
- самый реалистичный старт;
- официальные Windows APIs;
- не зависит от Wallpaper Engine API;
- хорошо подходит для первого рабочего прототипа.

Минусы:
- без анимации;
- не копирует спецэффекты Wallpaper Engine.

### Вариант B. Делать animated background внутри IDE самостоятельно

Схема:
- плагин сам декодирует видео / изображения / WebP / GIF / последовательность кадров;
- рисует их как background через внутренний painter path.

Плюсы:
- полный контроль;
- можно сделать кросс-IDE решение без зависимости от Wallpaper Engine.

Минусы:
- это уже не supported public API path;
- высокий риск по производительности;
- высокий риск регрессий между версиями IDE.

### Вариант C. Интеграция с Wallpaper Engine через его window/CLI

Схема:
- использовать CLI `openWallpaper ... -playInWindow`;
- дальше либо синхронизировать тот же wallpaper отдельно, либо пытаться захватывать окно.

Плюсы:
- можно использовать существующий wallpaper.

Минусы:
- в найденной официальной документации нет API для потоковой выдачи финальных кадров;
- захват окна это уже обходной путь, а не нормальная интеграция;
- сложность, лаги, прозрачность, синхронизация, DPI и multi-monitor резко усложняются.

### Вариант D. Поддержать только Web Wallpapers из Wallpaper Engine

Схема:
- брать только wallpapers типа `web`;
- читать их HTML/CSS/JS;
- рендерить внутри собственного JCEF/Chromium view либо адаптировать логику.

Плюсы:
- теоретически ближе к "официальному" содержимому Wallpaper Engine, чем захват картинки с экрана.

Минусы:
- это не равно поддержке Scene/Application wallpapers;
- нужен отдельный runtime и слой совместимости;
- не факт, что Wallpaper Engine-specific JS API будет воспроизводим в IDE без эмуляции.

## Рекомендованный порядок работ

1. Сначала сделать MVP для статичной картинки:
   - Windows only;
   - `IDesktopWallpaper`;
   - обновление background image в IDE.

2. Потом проверить обновление по таймеру:
   - реагировать на смену wallpaper;
   - поддержать multi-monitor policy;
   - понять, насколько хватает встроенного background path без собственного painter.

3. Только после этого исследовать анимацию:
   - либо собственный renderer;
   - либо узкий эксперимент с `wuyr/intellij-media-player` идеями;
   - отдельно оценить захват окна Wallpaper Engine как "грязный" путь.

## Основные ссылки

JetBrains:
- https://plugins.jetbrains.com/docs/intellij/intellij-platform.html
- https://plugins.jetbrains.com/docs/intellij/webstorm.html
- https://plugins.jetbrains.com/docs/intellij/rider.html
- https://plugins.jetbrains.com/docs/intellij/themes-getting-started.html
- https://plugins.jetbrains.com/docs/intellij/themes-extras.html
- https://plugins.jetbrains.com/docs/intellij/action-system.html
- https://www.jetbrains.com/help/idea/setting-background-image.html
- https://intellij-support.jetbrains.com/hc/en-us/community/posts/4413373216146-Changing-the-background-color-image-of-a-specific-editor

Examples:
- https://github.com/JetBrains/intellij-platform-plugin-template
- https://github.com/cocuh/intellij-wallpaper
- https://github.com/wuyr/intellij-media-player

Windows:
- https://learn.microsoft.com/en-us/windows/win32/api/shobjidl_core/nn-shobjidl_core-idesktopwallpaper
- https://learn.microsoft.com/en-us/windows/win32/api/shobjidl_core/nf-shobjidl_core-idesktopwallpaper-getwallpaper
- https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-systemparametersinfow

Wallpaper Engine:
- https://docs.wallpaperengine.io/en/web/overview.html
- https://help.wallpaperengine.io/en/functionality/cli.html
- https://help.wallpaperengine.io/en/functionality/export.html
- https://help.wallpaperengine.io/en/functionality/editingwallpapers.html
