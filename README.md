# Robot Path Finding — Java 21

Mô phỏng Robot tìm đường tránh chướng ngại vật bằng **BFS, Dijkstra và A*** trên bản đồ lưới.

## Chức năng chính
- **12 level dựng sẵn**: 6 mê cung + 6 bản đồ chướng ngại vật.
- Mê cung ngẫu nhiên với 5 mức độ khó và luôn kiểm tra khả năng tới Goal.
- Tự xây level: đặt Start/Goal, vẽ vật cản, đổi trọng số và lưu level.
- Lưu level vào `.robotpathfinding/levels` và nạp lại sau khi khởi động chương trình.
- Trọng số ô: `1 → 2 → 4 → 7`.
- BFS, Dijkstra, A* với animation trực quan.
- Thống kê **số bước, tổng chi phí, số node đã duyệt, thời gian tìm kiếm**.
- **So sánh 3 thuật toán** trên cùng một bản đồ.
- **Pause / tiếp tục / BƯỚC** để quan sát từng node trong animation.
- Điều chỉnh tốc độ mô phỏng 1x–5x.
- Hộp giải thích thuật toán BFS/Dijkstra/A*.
- Zoom bằng thanh trượt hoặc `Ctrl + lăn chuột`.
- Pan bản đồ bằng công cụ P.
- Hover chỉ làm nổi bật đúng ô dưới con trỏ.

## Phím tắt
- `S`: Đặt Start
- `D`: Đặt Goal
- `W`: Chướng ngại vật
- `T`: Đổi trọng số
- `P`: Pan bản đồ
- `Space`: Pause / tiếp tục
- `Ctrl + lăn`: Zoom

## Chạy

Yêu cầu **Java 21 trở lên**.

Windows PowerShell:

```powershell
Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName } | Set-Content sources.txt
javac -encoding UTF-8 -d out @sources.txt
java -cp out Main
```

## Ý nghĩa thống kê
- **Bước**: số cạnh trên đường đi.
- **Chi phí**: tổng cost của các ô robot đi qua sau Start.
- **Node**: số node được thuật toán lấy ra để xét.
- **Thời gian**: thời gian tìm kiếm của thuật toán, không bao gồm animation.

Lưu ý: BFS tối ưu số bước trên lưới không trọng số, còn Dijkstra/A* tối ưu tổng chi phí khi các ô có trọng số dương. Thời gian chạy có thể thay đổi giữa các lần chạy do JVM và máy tính.
