# ProjectE — Forge 1.20.1 → Fabric 26.2

這是 [ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte) 從 **Forge 1.20.1** 移植至 **Fabric 26.2** 的非官方專案。

ProjectE 是 Equivalent Exchange 2（EE2）的現代重製版，加入 EMC（Energy-Matter Covalence）系統、哲學之石、Klein Star、轉化桌、Collector、Relay、Condenser、各種戒指、護符與相關自動化內容。

> 本專案不是上游 ProjectE 官方的 Fabric 發布版本，也不代表上游維護者的立場或審核結果。

## 上游 ProjectE

本移植以原本的 ProjectE 為基礎，保留其核心玩法、內容、資源與 API 概念：

- CurseForge：[ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte)
- GitHub：[sinkillerj/ProjectE](https://github.com/sinkillerj/projecte)

請以原始 ProjectE 上游專案的說明、作者資訊與授權條款為準。本專案只負責 Forge 1.20.1 到 Fabric 26.2 的移植與相容性工作。

## AI 輔助移植聲明

本專案含有**大量 AI 輔助移植、轉換、整理與修復的程式碼**。AI 參與的工作包括但不限於：

- 將 Forge 1.20.1 API 與事件流程改寫為 Fabric API
- 將 capability、attachment、networking、registry、GUI 與 rendering 邏輯移植至 Fabric
- 協助處理 Minecraft 26.2 的方法簽章、Mappings 與 Java 25 相容性
- 根據既有實作、編譯錯誤、bytecode 與啟動記錄修復移植問題
- 整理反編譯或 API 轉換後的程式碼，使其可以繼續開發

因此，本專案的程式碼**不應被視為完全人工逐行重寫，也沒有經過上游 ProjectE 維護者審核**。即使目前可以建置，仍可能存在功能差異、邊界情況、模組相容性問題或需要人工重新檢查的移植程式碼。

如果你要使用、修改或發布本專案，請清楚標示：

1. 這是 ProjectE 的非官方 Fabric 26.2 移植。
2. 原始 ProjectE 來自 [sinkillerj/ProjectE](https://github.com/sinkillerj/projecte)。
3. 本專案包含大量 AI 輔助移植與修復程式碼。
4. 本專案不代表上游 ProjectE 官方 Fabric 支援。

## 版本與環境

| 項目 | 版本 |
|---|---|
| 來源版本 | Forge 1.20.1 ProjectE |
| 目標 Minecraft | 26.2 |
| Fabric Loader | 0.19.5 或更新版本 |
| Fabric API | 0.161.0+26.2 |
| Java | 25 或更新版本 |
| Fabric Loom | 1.17.21 |

## 主要內容

目前移植包含 ProjectE 的主要核心系統：

- EMC 計算、固定值、預生成 EMC 與自訂 EMC
- 哲學之石、Klein Star、Transmutation Table
- Energy Collector、Relay、Condenser
- DM/RM Furnace、Pedestal、Alchemical Bag 與 Alchemical Chest
- Mercurial Eye、Divining Rod、各種 Ring、Amulet、Talisman 與 Gem
- 玩家知識、EMC 儲存、同步與持久化
- Crafting、Smithing、Smelting、Special Recipe 與 World Transmutation mapper
- Client GUI、tooltip、搜尋、快捷鍵與 rendering overlay
- Fabric networking、Mixin、Access Widener、tags、配方、模型、貼圖、音效與語言檔

## 其他模組直接提供 EMC

其他 Fabric 模組不需要依賴 ProjectE API，也不需要玩家執行 `/projecte setemc`。只要在自己的模組 JAR 或 datapack 放置以下路徑即可：

```text
data/<你的模組 ID>/emc/*.json
```

最簡單的格式如下：

```json
{
  "comment": "My Mod EMC values",
  "values": {
    "mymod:custom_item": 128,
    "mymod:rare_item": 4096,
    "#mymod:custom_items": 256
  }
}
```

`values` 的 key 可以是物品 ID 或 item tag；以 `#` 開頭代表 tag。EMC 必須是大於或等於 0 的整數。每個檔案可以使用任意檔名，且可以在同一個模組中放置多個檔案。資料會在伺服器啟動及 datapack reload 時自動載入，因此不需要手動設定指令。

例如模組 `mymod` 的檔案應放在：

```text
data/mymod/emc/projecte.json
```

完整範例可參考 [`examples/emc/example.json`](examples/emc/example.json)。這個格式是 ProjectE 提供的公開、免 API 依賴格式，適合由其他模組直接內嵌到自己的 JAR 中。

### 自訂 EMC 指令與優先序

伺服器的 `custom_emc.json` 與 `/projecte setemc` 設定會優先於資料包預設值及自動礦石／原礦黑名單。
存在自訂設定時，重新映射會重新計算 EMC，避免被舊的預生成快取遮蓋。

```text
/projecte setemc 4096 minecraft:diamond
/projecte setemc minecraft:diamond 4096
/projecte setemc 256 #c:ores/iron
/projecte removeemc minecraft:diamond
/projecte resetemc minecraft:diamond
```

省略物品參數時，會使用主手物品；主手為空時使用副手。
手持物品與帶 components 的物品參數會保留精確變體設定。
`removeemc` 設為 0，`resetemc` 移除自訂設定並恢復資料包／自動映射結果。
設定、移除及重設後會立即儲存並重新計算 EMC，自動同步所有線上玩家的 EMC 與燃料排序。
轉化桌顯示也會隨新價格更新，無需再手動執行 `/reload`。

### 大型 EMC 資料與存檔保護

- EMC 價格表與玩家知識資料超過 32 KiB 時會分包同步，完整收齊並驗證校驗碼後才套用。
  傳輸中維持既有資料；逾時或驗證失敗會自動要求重新同步，重新連線也會取得完整快照。
- EMC 重算保留上一份完整價格表。映射或資料包讀取失敗時，不會用空表或半份計算結果覆蓋它。
- `config/ProjectE/custom_emc.json` 使用暫存檔、磁碟 flush 與原子替換儲存，並維護
  `custom_emc.json.bak`。原檔讀取失敗時會優先復原備份；損壞原檔保留為
  `custom_emc.corrupt-<UUID>.json`，不會直接改成空設定。
- 暫時缺少模組或無法辨識 components 的自訂價格會保留原始 entries，後續恢復模組時可重新讀取。
  已學物品暫時沒有 EMC 時只會停止提供轉化，不會自動刪除玩家的學習紀錄。
- 每個世界另存 `<世界目錄>/projecte/emc_last_good.json` 與其 `.bak`。若啟動時無法重新計算，
  且記憶體中沒有完整價格表，會嘗試使用該世界的最後有效快照。
- 大型分包同步需要 client 與 server 都換成包含此修正的 ProjectE JAR。

舊版已覆寫或刪除、且沒有任何備份的手動 EMC 價格無法憑空重建；需從原設定檔或存檔備份還原。

## EMI、REI 與 JEI 配方相容性

ProjectE 的一般合成配方使用 Minecraft 26.2 原生 recipe system，包含 shaped、shapeless、smithing、smelting 以及 ProjectE 的特殊配方。因此安裝相容於 Minecraft 26.2／Fabric 的 **EMI、REI 或 JEI** 後，可以直接查看 ProjectE 物品的合成方式與材料。

ProjectE 也會將自訂的 Klein Star 合成配方轉發為原生 `RecipeDisplay`。這讓 recipe viewer 可以顯示輸入物品、輸出物品和合成方式，而不需要 ProjectE 綁定特定 viewer 的 API。三種 viewer 可以擇一安裝，不需要同時安裝。

在 viewer 中通常可以使用以下功能：

- 對 ProjectE 物品按下查看配方快捷鍵，查看其合成材料。
- 對配方材料查看用途，例如 Dark Matter、Red Matter、Klein Star 和 Gem Armor 的使用方式。
- 查看 vanilla tag ingredient，例如石頭、黑曜石、鑽石方塊和染料等可替代材料。
- 查看 Klein Star、哲學之石及其他 ProjectE 自訂配方的合成顯示。

EMI、REI 和 JEI 的實際快捷鍵、搜尋方式與介面由各自模組決定。若某個配方仍未顯示，請先確認 viewer、Fabric API 及其他依賴都支援 Minecraft 26.2；這類 viewer 的版本相容性不是由 ProjectE 決定。

## Optional 整合

下列整合會在對應模組可用時啟用：

- Trinkets
- Jade
- The One Probe
- WTHIT
- EMI
- Roughly Enough Items（REI）
- Just Enough Items（JEI）

Jade 整合會顯示 ProjectE 機器的 EMC 緩衝與進度、凝聚器目標、熔爐輸入輸出、
收集器／中繼器目前處理的物品，以及暗物質台座的物品、啟用狀態、冷卻和效果說明。
準心指向裝有範圍效果物品的暗物質台座時，世界中會直接顯示作用邊界：
青綠色代表正在運作，琥珀色代表尚未啟用；Jade 同時列出半徑及完整長寬高。
天氣等全域效果不會顯示誤導性的範圍框。範圍預覽本身不要求安裝 Jade。

這些模組的 Minecraft 26.2／Fabric 相容性需由各自上游專案提供。未安裝 optional 模組時，ProjectE 核心內容不應依賴它們才能載入。

## 安裝

1. 安裝 Minecraft 26.2。
2. 安裝 Fabric Loader 0.19.5 或更新版本。
3. 安裝相容於 Minecraft 26.2 的 Fabric API。
4. 將 `ProjectE-1.2.0.jar` 放入 instance 的 `mods/` 目錄。
5. 視需求安裝 Trinkets、Jade、The One Probe、WTHIT，或擇一安裝 EMI、REI、JEI。
6. 使用 Java 25 啟動遊戲。

## 建置

### 需求

- JDK 25
- Git
- 可連線至 Fabric Maven、Maven Central 與相關依賴來源的環境

### 指令

```bash
# 編譯並打包
./gradlew build

# 清除後完整建置
./gradlew clean build --no-configuration-cache

# 只編譯 Java
./gradlew compileJava

# 啟動開發 client
./gradlew runClient
```

輸出檔案：

```text
build/libs/ProjectE-1.2.0.jar
build/libs/ProjectE-1.2.0-sources.jar
```

### Energy Condenser 回歸測試

凝聚器測試使用專案的 Gradle Wrapper、Fabric Loader JUnit 與 Fabric GameTest。
`build` 的 `check` 階段會同時執行單元測試與實際 Minecraft 伺服器測試。

在 Windows PowerShell 使用 JDK 25 時，先設定並確認實際使用的 JVM：

```powershell
java -version
.\gradlew.bat --version

# 底層物品插入／合併與模擬插入的守恆測試
.\gradlew.bat test --no-configuration-cache

# MK1／MK2 凝聚器、容器、封包及存檔的伺服器回歸測試
.\gradlew.bat runGameTest --no-configuration-cache

# 包含上述測試的完整乾淨建置
.\gradlew.bat clean build --no-configuration-cache
```

測試涵蓋高低 EMC 目標互換、舊輸出重新轉換、快速切換、重複／過期點擊封包、
無 EMC 目標、Shift 點擊、自動化、輸出空間與物品 components、GUI 重開同步、
EMC 重映射、區塊卸載後磁碟重載及 NBT 存檔資料重建。
使用 `BigInteger` 檢查輸入、輸出物品價值與累積 EMC 的總額，避免測試本身溢位。

- 單元測試報告：`build/reports/tests/test/index.html`
- 伺服器測試報告：`build/run/gameTest/build/gametest-results.xml`
- 伺服器測試記錄：`build/run/gameTest/logs/latest.log`

測試用模組與 EMC 資料位於 `src/gametest/`，不會打包進正式的 ProjectE JAR。
漏洞原因及修復流程見 [`docs/energy-condenser-fix.md`](docs/energy-condenser-fix.md)。

### 破壞類物品的經驗掉落

毀滅燧石、催化水晶及暗物質／紅物質工具的範圍、連鎖與多方塊挖掘，
會在方塊成功移除後執行原生掉落回呼，保留礦石、刷怪籠、伏聆方塊等原本會掉落的經驗球。
墨丘利之眼替換無 EMC 方塊並產生普通掉落時，也會保留對應的經驗。

經驗量依方塊自己的實作與實際工具決定，遵守絲綢之觸、創造模式及方塊掉落遊戲規則。
取消破壞、燃料不足或重複對已移除方塊操作不會產生經驗。

`src/gametest/java/moze_intel/projecte/gametest/BlockBreakingGameTests.java`
涵蓋這些情境；使用上面的 `runGameTest` 或 `clean build` 指令會一併執行。

### 箱子自動化與破壞掉落

煉金箱及 MK1／MK2 凝聚器提供原生容器與 Fabric `ItemStorage.SIDED` 介面，
支援漏斗輸入／輸出、漏斗礦車抽取、投擲器輸入，以及使用 Fabric Transfer API 的外部設備。
凝聚器保留原本的輸入／輸出限制，幽靈目標不會被抽取或掉落。

箱子破壞時會掉落全部實際庫存並保留物品 components；
收集器、繼電器、物質熔爐及基座也會在方塊實體移除前處理庫存掉落。
輸出已滿或 Fabric 交易取消時保留原物品，方塊替換遭取消時恢復庫存與 EMC。

`src/gametest/java/moze_intel/projecte/gametest/ChestAutomationGameTests.java`
使用原生漏斗、投擲器、礦車、玩家破壞及放置取消事件驗證數量守恆。
EMC 指令、tag、components、重映射及快取回歸位於 `EmcCommandGameTests.java`。
兩組測試也會隨 `runGameTest` 或 `clean build` 執行。

### 轉化桌與玩家物品欄的物品守恆

轉化桌 Shift 點擊只會取得 EMC 實際可購買且物品欄能容納的數量，並依真正插入的數量扣款。
玩家身上已有同類物品時，合併只加入實際購買數量；永恆密度寶石的玩家物品欄輸出也使用這個守恆流程。
EMC 重映射後已變成無價值的舊顯示項目，不能透過點擊、快捷列交換或丟棄取得免費物品。

`src/gametest/java/moze_intel/projecte/gametest/TransmutationGameTests.java`
涵蓋 Shift＋右鍵、重複點擊封包、滿物品欄、Klein Star 扣款、寶石輸出、components 合併與無效抽取。

## 原始碼結構

```text
src/main/java/moze_intel/projecte/
├── api/             # ProjectE 對外 API、NSS、EMC mapper 與資料 builder
├── client/          # Fabric client entrypoint、GUI、按鍵與 client 初始化
├── compat/          # Forge API 移植後的 Fabric/通用相容層
├── config/          # client、common、server 與 mapping 設定
├── emc/             # EMC arithmetic、collector、mapper 與 processor
├── events/          # Fabric event 與 ProjectE event handler
├── gameObjs/        # blocks、items、block entities、menus 與 registries
├── impl/            # capability、codec、attachment 與核心實作
├── integration/     # optional mod integrations
├── mixin/           # ProjectE Mixin
├── network/         # client/server packet 與同步邏輯
├── rendering/       # renderer 與 transmutation overlay
└── utils/            # 工具、文字、世界、EMC 與遊戲邏輯輔助類別

src/main/resources/
├── fabric.mod.json
├── projecte.mixins.json
├── projecte.accesswidener
├── assets/projecte/
└── data/projecte/
```

## WTHIT bytecode 說明

WTHIT 的 API 在目前 Fabric 26.2 開發依賴中尚未固定，因此正式 JAR 會保留新版相容的 WTHIT runtime class，位於：

```text
vendor/wthit-classes/
```

這是為了維持 optional WTHIT 功能，不是一般建置快取或編譯垃圾。對應的移植 source 需要搭配相容的 WTHIT API 後才能重新編譯。

## 開發注意事項

這是一個跨 loader、跨 Minecraft 版本的移植專案。修改時請特別檢查：

- Fabric registry 與 vanilla registry lifecycle
- Forge capability 對應的 Fabric attachment／component 行為
- client/server packet 對稱性
- Mixin target 與 Access Widener
- EMC mapper 的 reload 順序與資料包相容性
- container、GUI 與 rendering 的 client-only class
- optional integration 不應在缺少相依模組時載入核心類別

修改後至少執行：

```bash
./gradlew clean build --no-configuration-cache
```

能夠編譯不等於已完成遊戲功能驗證；重要功能仍應在實際 Minecraft instance 中測試。

## 問題回報

請附上以下資訊：

- Minecraft、Fabric Loader、Fabric API 與 Java 版本
- ProjectE 版本
- 其他已安裝模組
- `latest.log` 或 crash report
- 重現步驟
- 問題發生在 Forge 來源功能、Fabric 移植層，或 optional integration

如果問題屬於 ProjectE 原始功能，請同時查看上游：

<https://github.com/sinkillerj/projecte>

## 授權與致謝

本專案以原本 ProjectE 為上游基礎。請閱讀 [`LICENSE`](LICENSE) 與 [`LICENSE_ProjectE`](src/main/resources/LICENSE_ProjectE)，並遵守上游 ProjectE 的授權與作者資訊。

- 上游 CurseForge：<https://www.curseforge.com/minecraft/mc-mods/projecte>
- 上游 GitHub：<https://github.com/sinkillerj/projecte>
