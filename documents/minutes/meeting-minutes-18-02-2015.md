Meeting minutes 18/02/2015
==

Issues
===
- Dòng thứ 3 của icon là サーバー  (hiện nay e để là サー, thiếu chữ バー). Dòng 1 và 2 đúng rồi (em copy full text từ cái icon ra hoặc copy từ email này là OK).
- Nút "Back" ở màn hình Play/Record là lùi lại một tí khi đang chơi file record (lùi bao nhiêu config trong màn hình Settings), chứ không phải là bấm vào đó quay về màn hình trước.

Demo next Friday
===
- Fix app icon
- Hoàn thiện toàn bộ UI, string.xml
- Highlight selected tab
- Fix list action button

Wishlist
===
- Hoàn thiện phần Play (từ Fast, Slow, Timer, Back)
- Hoàn thiện Recorded Programs
- Token

Radiko, NHK Program list API
===

Dưới đây là các API để lấy Tên và Description của chương trình nhé :

* Radiko :
http://radiko.jp/v2/api/program/today?area_id=JP13
Trong đó area_id là tham số em nhận được khi auth2 (nếu không phải ở vùng Tokyo nó sẽ là JPXX gì đó)

API trên sẽ trả về thông tin (XML) cho toàn bộ các chương trình của Radiko có thể nghe được ở vùng area_id trong ngày hôm đó (chỉ cần gọi 1 lần / 1 ngày).

* NHK :

http://api.nhk.or.jp/v1/pg/list/130/r1/2015-02-18.json?key=EHtQCGQoSMcYRA1AImJjcJljFGoIqdwG

Dạng tổng quát :
http://api.nhk.or.jp/v1/pg/list/{area}/{service}/{date}.json?key={apikey}

- Area : 
http://api-portal.nhk.or.jp/doc-request#explain_area
(Tokyo = 130)

- Service :
http://api-portal.nhk.or.jp/doc-request#explain_service
Ví dụ : r1 (Radio 1)

Em có thể thử trực tiếp ở đây :
http://api-portal.nhk.or.jp/doc_list-v1_con

Trước mắt chỉ cần 2 cái trên là OK.
Có lẽ cần làm 1 server (PHP?) để trả về thông tin các kênh là dễ nhất, vì NHK nó chỉ cho gọi API 300 lần / 1 ngày / 1 API Key