# Energy Condenser 物品複製修復

## 根因與 Forge 比對

主要參考是相鄰的 `../ProjectE` 原版程式碼，以及該專案實際使用的
NeoForge 21.1.148 `ItemStackHandler`／`ItemHandlerHelper` 原始碼。

兩個專案的正常呼叫鏈為：

1. `AbstractCondenserScreen` 顯示凝聚器選單與 EMC 進度。
2. 原生容器點擊封包在伺服器執行 `CondenserContainer.clicked`。
3. 目標槽是 `SlotCondenserLock` 虛擬槽，不消耗或提供玩家物品。
4. `attemptCondenserSet` 使用**伺服器選單的游標 ItemStack**，透過
   `IEMCProxy.getPersistentInfo` 保留原版允許的 components，並觸發可取消的設定事件。
5. 設定／清除目標強制更新 `requiredEmc` 與 EMC 接收狀態。
   `ItemInfo` 不保存數量，每次 `createStack()` 生成一個物品。
6. `CondenserBlockEntity.tickServer` 更新映射快取；MK1 每 tick 消耗一個輸入，
   MK2 先使用已累積 EMC 生成輸出，再消耗一個輸入堆疊。
7. 輸入使用 `getSellValue`，輸出使用完整 `getValue`。
8. `ItemHandlerHelper.insertItemStacked` 將輸出交給 `ItemStackHandler.insertItem`。
9. 原生容器同步實際物品槽；`UpdateCondenserLockPKT` 與 `UpdateWindowLongPKT`
   是伺服器到客戶端的顯示同步。
10. `emc`、`input`、`lock`、MK2 的 `output` 透過既有 NBT／ValueInput 流程儲存。

**主要漏洞位於步驟 8。** Fabric 相容層在合併非空堆疊時使用
`existing.grow(limit)`，其中 `limit` 是剩餘容量，而非本次插入數量。
原版使用 `existing.grow(reachedLimit ? limit : stack.getCount())`。

因此，已有 1 個鑽石再生成 1 個鑽石時，移植版會增加 63 個、變成 64 個，
但凝聚器只支付 1 個鑽石的 EMC。模擬插入的回傳值又與實際插入不一致。
MK1 切換目標後會將舊輸出視為輸入，把免費多出的物品轉回 EMC；MK2 的免費輸出
也可被玩家或自動化重新投入，讓反覆切換持續放大物品價值。

## 修復

- 修正底層合併數量，並補回原版的 `isItemValid` 插入驗證。
- 堆疊插入使用與原版一致的 item／components 比較。
- MK1／MK2 共用 `produceOutput`：確認伺服器端、正 EMC 成本、資金足夠、
  一個目標物品的實際 EMC，以及完整插入可行性；保留一筆成本後執行插入，
  拒絕插入時退回該筆成本。
- 空間判斷使用實際輸出堆疊的模擬插入，避免「persistent target 相等」但
  components 不可堆疊或槽位限制不同時，扣款卻沒有產出。
- 輸入 EMC 依實際取出的數量計算；MK2 將消耗數量限制在 EMC 儲存可容納範圍內，
  防止高 EMC 堆疊乘法溢位或刪除尚未存入的 EMC。
- 載入資料到既有方塊實體時清除舊成本／接收快取，重新驗證新目標；缺少輸入／輸出
  欄位時正確清空舊庫存，避免沿用之前的狀態。
- 目標顯示同步只能修改客戶端方塊實體；伺服器設定需對應目前有效的凝聚器選單。

## 保留的原版行為

- 清除或切換目標保留全部累積 EMC，不把低價目標的進度比例套用到高價目標。
- 原版的「點一下清除，再以游標物品設定」操作、可取消設定事件及虛擬目標槽。
- MK1／MK2 的庫存配置、每 tick 處理順序、生成速度與 EMC 損耗設定。
- 無 EMC 目標不生成、不接收 EMC，但保留內部目標資料以便重映射後恢復。
- 原有 GUI、components 持久化規則及存檔欄位與編碼格式。

## 驗證方式

`src/test/.../ItemStackHandlerTest.java` 直接測試真實 ItemStack／handler，
涵蓋不同容量、輸入數量、模擬插入、components 與不可堆疊物品。
修復前的回歸測試確認了「預期 2 個鑽石，實際 64 個」的錯誤。

`src/gametest/.../CondenserGameTests.java` 在 Minecraft 26.2 的真正 Fabric
伺服器執行，透過實際凝聚器、選單、EMC proxy、原生點擊封包、資料包 reload、
NBT 與磁碟區塊生命週期驗證。測試總價值計算使用 `BigInteger`，並避免將 MK1
共用的輸入／輸出庫存重複計入。

執行指令及報告位置見 README 的「Energy Condenser 回歸測試」。
