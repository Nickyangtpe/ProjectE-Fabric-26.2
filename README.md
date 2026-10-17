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
| 移植版本 | 1.2.0 |

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
