# LỜI MỞ ĐẦU

Trong những năm gần đây, ngành du lịch Việt Nam phát triển nhanh chóng cùng với sự phổ biến của thương mại điện tử. Người dùng ngày càng có xu hướng tìm hiểu, so sánh và đặt tour du lịch trực tuyến thay vì phải đến trực tiếp các phòng vé hay công ty lữ hành. Việc xây dựng một website cho phép giới thiệu tour và đặt tour trực tuyến không chỉ giúp doanh nghiệp lữ hành tiếp cận khách hàng rộng hơn mà còn giúp khách hàng chủ động tra cứu lịch trình, giá cả, số chỗ còn trống và hoàn tất việc đặt chỗ chỉ trong vài phút.

Đồ án "Website giới thiệu tour du lịch và cho phép đăng ký tour trực tuyến" (TourBooking) được thực hiện trong khuôn khổ môn học **Lập trình WWW (Java)** tại Khoa Công nghệ Thông tin, Trường Đại học Công nghiệp Thành phố Hồ Chí Minh. Đồ án là dịp để nhóm vận dụng các kiến thức đã học về lập trình Java và cơ sở dữ liệu quan hệ vào một bài toán có thật: một website thương mại điện tử cỡ vừa, có luồng nghiệp vụ đặt hàng hoàn chỉnh, có phân quyền nhiều vai trò và có khu vực quản trị riêng.

Hệ thống TourBooking gồm hai khu vực chính: các trang phục vụ khách truy cập (xem, tìm kiếm, đặt tour, quản lý tài khoản) và một khu vực quản trị riêng dành cho nhân viên vận hành, có hỗ trợ các thao tác cập nhật nhanh không cần tải lại trang (giỏ hàng, kiểm tra mã giảm giá, xem biểu đồ thống kê, chọn đợt khởi hành). Dữ liệu được tổ chức thành 17 bảng có quan hệ chặt chẽ với nhau; toàn bộ quy tắc nghiệp vụ - đặc biệt là các quy tắc chặn xoá dữ liệu khi còn ràng buộc - được kiểm tra ngay trong chương trình thay vì phó mặc cho cơ sở dữ liệu, đúng theo yêu cầu của đề bài.

Báo cáo này trình bày toàn bộ quá trình thực hiện đồ án: từ khảo sát và phân tích yêu cầu, xây dựng mô hình use case và mô hình lớp, thiết kế cơ sở dữ liệu, đến hiện thực các chức năng và kiểm thử hệ thống. Báo cáo cũng nêu rõ những khó khăn kỹ thuật thực sự gặp phải trong quá trình xây dựng (ví dụ: tìm kiếm tiếng Việt không dấu, chống bán quá chỗ khi nhiều người đặt cùng lúc, giữ giỏ hàng sống sót qua phiên đăng nhập) cùng cách nhóm đã giải quyết.

Do thời gian và nguồn lực có hạn, sản phẩm khó tránh khỏi thiếu sót. Nhóm thực hiện mong nhận được sự góp ý của giảng viên để hoàn thiện đồ án cũng như rút kinh nghiệm cho các dự án sau này.

# CHƯƠNG 1: GIỚI THIỆU

## 1.1 Tổng quan

Thị trường du lịch trực tuyến tại Việt Nam đang tăng trưởng mạnh, nhưng phần lớn các công ty lữ hành quy mô vừa và nhỏ vẫn giới thiệu tour qua các kênh rời rạc: fanpage mạng xã hội, file PDF gửi qua Zalo, hoặc một website tĩnh chỉ có chức năng "liên hệ tư vấn". Khách hàng muốn biết chính xác còn bao nhiêu chỗ trống cho một đợt khởi hành cụ thể, giá người lớn và trẻ em khác nhau ra sao, hay muốn dùng thử một mã giảm giá trước khi quyết định, đều phải nhắn tin hỏi trực tiếp nhân viên tư vấn - vừa chậm, vừa phụ thuộc vào giờ làm việc của nhân viên.

TourBooking được xây dựng để giải quyết đúng khoảng trống đó: một website cho phép khách vãng lai xem và tìm kiếm tour công khai, khách hàng đã đăng ký có thể tự đặt tour trực tuyến với đầy đủ thông tin đợt khởi hành - giá - số chỗ hiển thị theo thời gian thực, còn quản trị viên có một khu vực riêng để quản lý toàn bộ danh mục tour, đợt khởi hành, đơn đặt, tài khoản và xem báo cáo doanh thu.

## 1.2 Mục tiêu đề tài

Mục tiêu chính của đồ án là xây dựng một website hoàn chỉnh, chạy được thật, cho phép:

- Khách vãng lai xem, tìm kiếm và lọc tour theo nhiều tiêu chí mà không cần đăng nhập.
- Khách hàng đăng ký tài khoản, đặt tour trực tuyến theo từng đợt khởi hành cụ thể, quản lý hồ sơ cá nhân và xem lại lịch sử đơn đặt của chính mình.
- Quản trị viên quản lý toàn bộ dữ liệu vận hành: danh mục, tour, ảnh, đợt khởi hành, đơn đặt, tài khoản người dùng, mã khuyến mãi, và theo dõi doanh thu qua biểu đồ trực quan.
- Toàn bộ quy tắc nghiệp vụ quan trọng (chặn xoá dữ liệu đang được tham chiếu, chống bán quá chỗ khi nhiều người đặt cùng lúc, giới hạn mức giảm giá) được thực thi ở tầng ứng dụng bằng Java, không phó mặc cho ràng buộc của hệ quản trị cơ sở dữ liệu.

## 1.3 Phạm vi đề tài

Đề tài tập trung vào nhóm chức năng "giới thiệu tour và đặt tour trực tuyến" dành cho ba nhóm tác nhân: khách vãng lai, khách hàng đã đăng ký, và quản trị viên. Hệ thống không xử lý thanh toán thật qua cổng thanh toán (ngân hàng, ví điện tử) mà chỉ ghi nhận hình thức thanh toán do khách chọn; việc gửi email xác nhận đơn dùng một dịch vụ email giả lập ghi ra console thay vì gửi SMTP thật, có thể đổi sang SMTP thật chỉ bằng một dòng cấu hình. Đề tài cũng không bao gồm ứng dụng di động, chỉ có phiên bản web đáp ứng (responsive) chạy trên trình duyệt máy tính và điện thoại.

## 1.4 Mô tả yêu cầu chức năng

Hệ thống cần đáp ứng các nhóm chức năng chính sau:

- **Nhóm công khai:** xem trang chủ, xem trang giới thiệu, tìm kiếm và lọc tour (theo danh mục, điểm đến, khoảng giá, từ khoá không dấu), xem chi tiết một tour kèm lịch trình từng ngày và ảnh, quản lý giỏ hàng tạm thời trong phiên làm việc, đăng ký tài khoản, đăng nhập và đăng xuất.
- **Nhóm khách hàng:** đặt tour trực tuyến (chọn đợt khởi hành, số người lớn/trẻ em, áp mã giảm giá), xem lại lịch sử và chi tiết đơn đã đặt, cập nhật hồ sơ cá nhân, đổi mật khẩu, đánh giá tour đã đi.
- **Nhóm quản trị:** quản lý danh mục tour, quản lý tour và ảnh minh hoạ, quản lý đợt khởi hành của từng tour, quản lý đơn đặt tour (đổi trạng thái, sửa số khách, huỷ đơn), quản lý tài khoản người dùng, quản lý mã khuyến mãi, xem thống kê doanh thu theo tháng.

Mọi thao tác xoá dữ liệu đang được tham chiếu (xoá danh mục còn tour, xoá tour đã có người đặt hoặc có đánh giá, xoá đợt khởi hành đã có người đặt, xoá tài khoản còn đơn hoặc là quản trị viên cuối cùng) đều phải bị chặn kèm thông báo rõ ràng bằng tiếng Việt, có gợi ý phương án thay thế (ví dụ "ngừng bán" thay vì "xoá").

# CHƯƠNG 2: PHÂN TÍCH

## 2.1 Thu thập yêu cầu

### 2.1.1 Khảo sát hiện trạng

Trước khi bắt tay xây dựng hệ thống, nhóm đã khảo sát cách các công ty lữ hành quy mô vừa và nhỏ đang giới thiệu và bán tour hiện nay, thông qua việc quan sát một số website và fanpage bán tour phổ biến, đồng thời tham khảo góp ý của giảng viên hướng dẫn về đặc thù của đề tài "giới thiệu tour du lịch và đăng ký tour trực tuyến" (nhóm đề tài 51-54).

Kết quả khảo sát cho thấy phần lớn các đơn vị lữ hành nhỏ vẫn giới thiệu tour bằng các bài đăng rời rạc trên mạng xã hội hoặc một trang web tĩnh chỉ có thông tin liên hệ, không có chức năng đặt chỗ trực tuyến thật sự. Khách muốn biết một đợt khởi hành cụ thể còn bao nhiêu chỗ trống phải nhắn tin hỏi trực tiếp, dẫn đến độ trễ phản hồi lớn và rủi ro nhận nhầm thông tin (nhân viên tư vấn nhớ nhầm số chỗ còn trống, báo giá sai cho người lớn/trẻ em). Khi hai khách cùng hỏi đặt một đợt khởi hành gần hết chỗ trong cùng thời điểm, việc xác nhận thủ công qua tin nhắn rất dễ dẫn đến bán vượt quá số chỗ thực có - đây chính là rủi ro nghiệp vụ mà hệ thống cần giải quyết bằng một cơ chế giữ chỗ đáng tin cậy hơn con người.

Bên cạnh đó, việc không có tài khoản khách hàng khiến người dùng phải cung cấp lại toàn bộ thông tin liên hệ mỗi lần đặt tour mới, và không có cách nào để tự tra cứu lại lịch sử các lần đặt trước đó. Về phía người quản lý, việc theo dõi doanh thu, số lượt đặt theo từng tour hay từng tháng đều làm thủ công trên sổ sách hoặc bảng tính, không có báo cáo trực quan theo thời gian thực.

### 2.1.2 Quy trình nghiệp vụ

**Quy trình đặt tour trực tuyến (Booking Process)**

Đây là quy trình trung tâm của hệ thống, yêu cầu chính xác tuyệt đối về số chỗ còn lại và số tiền phải trả.

Bước 1: Tìm và chọn tour. Khách tìm kiếm hoặc duyệt danh mục tour, mở trang chi tiết một tour để xem lịch trình, ảnh, và danh sách các đợt khởi hành kèm giá người lớn/trẻ em và số chỗ còn trống.

Bước 2: Thêm vào giỏ hàng. Khách chọn một đợt khởi hành cụ thể, nhập số người lớn và số trẻ em, hệ thống thêm dòng tương ứng vào giỏ hàng lưu trong phiên làm việc (session). Nếu khách chọn lại đúng đợt khởi hành đã có trong giỏ, hệ thống cộng dồn số khách vào dòng cũ thay vì tạo dòng mới.

Bước 3: Quản lý giỏ hàng. Khách có thể xem lại giỏ, sửa số lượng khách của từng dòng (sửa về 0 thì dòng tự biến mất khỏi giỏ), hoặc xoá hẳn một dòng, hoặc làm rỗng cả giỏ.

Bước 4: Xác thực tài khoản. Hệ thống yêu cầu đăng nhập trước khi cho vào trang thanh toán; nếu chưa có tài khoản, khách phải đăng ký trước. Giỏ hàng đang có phải được giữ nguyên qua bước đăng nhập, không được biến mất.

Bước 5: Điền thông tin thanh toán. Khách xác nhận lại thông tin liên hệ (họ tên, email, số điện thoại, địa chỉ - được điền sẵn từ hồ sơ nhưng vẫn sửa được), chọn hình thức thanh toán, có thể nhập mã giảm giá để hệ thống tính lại số tiền phải trả.

Bước 6: Xác nhận và ghi đơn. Hệ thống tính lại toàn bộ số tiền từ dữ liệu thật trong cơ sở dữ liệu (không tin số liệu do trình duyệt gửi lên), kiểm tra lại số chỗ còn trống của từng đợt khởi hành trong giỏ, trừ số chỗ, sinh mã đơn, ghi nhận lượt dùng mã giảm giá nếu có, rồi làm rỗng giỏ hàng. Toàn bộ các thao tác này nằm trong **một giao dịch (transaction) duy nhất**: nếu bất kỳ bước nào thất bại (ví dụ một đợt khởi hành vừa hết chỗ do người khác đặt trước), toàn bộ đơn không được ghi một phần nào xuống cơ sở dữ liệu.

Bước 7: Xác nhận thành công. Hệ thống hiển thị trang xác nhận kèm mã đơn, đồng thời gửi thông báo xác nhận (email giả lập ghi ra console trong môi trường phát triển).

**Quy trình xử lý đơn đặt tour phía quản trị (Booking Management Process)**

Bước 1: Quản trị viên xem danh sách đơn đặt, lọc theo trạng thái (chờ xác nhận, đã xác nhận, hoàn thành, đã huỷ).

Bước 2: Mở chi tiết một đơn để xem đầy đủ thông tin khách, các dòng đợt khởi hành đã đặt, số tiền, lịch sử đổi trạng thái.

Bước 3: Đổi trạng thái đơn theo vòng đời hợp lệ (chờ xác nhận → đã xác nhận → hoàn thành, hoặc huỷ ở bất kỳ bước nào trước khi hoàn thành). Mỗi lần đổi trạng thái được ghi vào nhật ký lịch sử kèm thời điểm và người thực hiện.

Bước 4: Nếu huỷ đơn, hệ thống tự động hoàn lại số chỗ đã giữ cho các đợt khởi hành liên quan. Quản trị viên cũng có thể sửa lại số lượng khách của một dòng trong đơn - khi đó thành tiền của dòng, tổng tiền của đơn và số chỗ còn trống của đợt khởi hành phải được tính lại đồng thời và luôn khớp nhau.

**Quy trình quản lý danh mục - tour - đợt khởi hành (Catalog Management Process)**

Bước 1: Quản trị viên tạo danh mục tour (ví dụ "Du lịch biển", "Du lịch nước ngoài").

Bước 2: Tạo tour thuộc một danh mục, nhập thông tin chung (tên, mô tả, điểm khởi hành, điểm đến, số ngày/đêm, giá tham khảo), tải lên nhiều ảnh minh hoạ, nhập lịch trình từng ngày.

Bước 3: Tạo các đợt khởi hành cho tour: ngày đi, ngày về, tổng số chỗ, giá người lớn, giá trẻ em. Mỗi đợt khởi hành độc lập về giá và số chỗ, cùng một tour có thể mở nhiều đợt với giá khác nhau theo mùa.

Bước 4: Khi cần ngừng bán một danh mục, một tour hay một đợt khởi hành đã từng phát sinh giao dịch, hệ thống **từ chối xoá** và gợi ý chuyển sang trạng thái "ngừng hoạt động"/"ngừng bán" - dữ liệu lịch sử của các đơn đã đặt không bao giờ bị mất tham chiếu.

### 2.1.3 Quy định

**Ràng buộc nghiệp vụ (Business Rules)**

BR-01: Kiểm soát số chỗ còn trống. Hệ thống không được xác nhận một đơn đặt nếu số chỗ còn trống của đợt khởi hành nhỏ hơn tổng số khách yêu cầu. Việc trừ chỗ phải diễn ra trọn vẹn cùng lúc với việc ghi đơn, và phải chống được tình huống hai khách đặt cùng lúc vào đợt khởi hành sắp hết chỗ.

BR-02: Đơn hàng là chứng từ, không được đổi theo dữ liệu gốc về sau. Tên tour, đơn giá người lớn/trẻ em tại thời điểm đặt phải được chép lại vào chi tiết đơn; sau này quản trị viên đổi tên tour hay đổi giá của đợt khởi hành thì các đơn cũ vẫn phải hiển thị đúng số liệu lúc đặt.

BR-03: Chặn xoá dữ liệu còn được tham chiếu. Không cho xoá danh mục còn tour, không cho xoá tour đã có lượt đặt/đánh giá/liên hệ, không cho xoá đợt khởi hành đã có lượt đặt, không cho xoá tài khoản còn đơn/đánh giá/lượt dùng mã giảm giá, không cho tự xoá chính mình, không cho xoá quản trị viên cuối cùng. Mọi trường hợp bị chặn đều phải trả về thông báo rõ ràng và gợi ý phương án thay thế; quy tắc này phải được chương trình chủ động kiểm tra trước khi thao tác xảy ra, không phó mặc cho cơ sở dữ liệu báo lỗi thay.

BR-04: Mã giảm giá có điều kiện áp dụng rõ ràng. Mã phải còn hiệu lực về thời gian, còn lượt sử dụng, đơn phải đạt giá trị tối thiểu, và mỗi tài khoản chỉ được dùng một mã đúng một lần. Mức giảm theo phần trăm phải có trần tối đa để tránh giảm quá nhiều trên đơn giá trị lớn.

**Ràng buộc kỹ thuật và phi chức năng**

TR-01: Trải nghiệm người dùng. Giao diện phải rõ ràng, có phân trang cho danh sách tour và đơn hàng, giữ nguyên bộ lọc khi chuyển trang, hỗ trợ đa ngôn ngữ Việt/Anh, và tìm kiếm phải ra kết quả đúng kể cả khi người dùng gõ không dấu.

TR-02: Bảo mật và phân quyền. Mật khẩu người dùng phải được mã hoá, không bao giờ hiển thị hay truyền đi dưới dạng chữ thường. Hệ thống phải phân quyền theo vai trò: mọi khu vực quản trị chỉ dành cho quản trị viên; khách hàng chỉ được xem đơn của chính mình, mở đơn của người khác phải bị từ chối. Toàn bộ biểu mẫu và thao tác thay đổi dữ liệu phải được bảo vệ chống giả mạo yêu cầu.

TR-03: Toàn vẹn và nhất quán dữ liệu. Nghiệp vụ đặt tour phải được xử lý trọn vẹn trong một lần duy nhất, không được để lại trạng thái nửa vời khi có lỗi giữa chừng. Mọi ràng buộc nghiệp vụ phải được kiểm tra ngay trong chương trình ứng dụng, tuyệt đối không đặt ràng buộc kiểm tra dữ liệu, hàm tính toán, thủ tục lưu trữ hay cơ chế tự động kích hoạt ở tầng cơ sở dữ liệu.

## 2.2 Danh sách câu hỏi khi thu thập yêu cầu

| STT | Câu hỏi | Trả lời | Ghi chú |
|---|---|---|---|
| 1 | Một tour có thể có nhiều đợt khởi hành với giá khác nhau không? | Có. Cùng một chương trình tour (ví dụ "Đà Lạt 3N2Đ") có thể mở nhiều đợt khởi hành khác ngày, mỗi đợt có giá người lớn/trẻ em và số chỗ riêng, tách biệt hoàn toàn khỏi thông tin chung của tour. |  |
| 2 | Khi hai khách cùng đặt một đợt khởi hành sắp hết chỗ ở cùng thời điểm thì xử lý ra sao? | Hệ thống phải kiểm tra và trừ số chỗ ngay trong giao dịch ghi đơn; ai xác nhận thành công trước sẽ được giữ chỗ, người sau nếu không đủ chỗ phải nhận thông báo lỗi rõ ràng, tuyệt đối không để số chỗ còn lại âm. |  |
| 3 | Khách vãng lai (chưa đăng nhập) có được dùng giỏ hàng và xem tour không? | Có. Xem tour, tìm kiếm và dùng giỏ hàng không đòi hỏi đăng nhập; chỉ khi bấm "Đặt tour" ở bước thanh toán mới bắt buộc có tài khoản, và giỏ hàng đang có phải giữ nguyên qua bước đăng nhập đó. |  |
| 4 | Giá tour có tính riêng cho trẻ em không? | Có. Mỗi đợt khởi hành có giá người lớn và giá trẻ em riêng biệt; thành tiền của một dòng đặt tour bằng số người lớn nhân giá người lớn cộng số trẻ em nhân giá trẻ em. |  |
| 5 | Mã giảm giá áp dụng như thế nào, có giới hạn gì không? | Mỗi mã có thời gian hiệu lực, tổng số lượt dùng, số lượt tối đa cho mỗi tài khoản, giá trị đơn tối thiểu, và với mã giảm theo phần trăm thì có mức giảm tối đa để tránh giảm quá lớn trên đơn giá trị cao. |  |
| 6 | Khách hàng có xem lại được lịch sử đặt tour của mình không? | Có. Khách hàng đã đăng nhập xem được danh sách và chi tiết các đơn mình đã đặt; hệ thống phải kiểm tra quyền sở hữu, khách khác không thể xem đơn của người khác dù biết đường dẫn. |  |
| 7 | Nếu quản trị viên muốn ngừng bán một tour đã có khách đặt thì làm sao, có xoá được không? | Không được xoá. Hệ thống chặn thao tác xoá và gợi ý chuyển tour/đợt khởi hành sang trạng thái "ngừng hoạt động" để giữ nguyên dữ liệu của các đơn đã đặt trong quá khứ. |  |
| 8 | Khách đánh giá tour khi nào, ai được đánh giá? | Chỉ khách hàng đã thực sự đặt tour đó và đơn đã ở trạng thái hoàn thành mới được đánh giá; mỗi tài khoản chỉ đánh giá một tour đúng một lần. |  |
| 9 | Quản trị viên cần xem những số liệu gì để theo dõi tình hình kinh doanh? | Cần biểu đồ doanh thu theo tháng, tổng số đơn, số tour đang bán, và các bộ lọc theo trạng thái đơn để xử lý công việc hằng ngày. |  |
| 10 | Website có hỗ trợ nhiều ngôn ngữ không? | Có. Hỗ trợ tiếng Việt và tiếng Anh, người dùng đổi ngôn ngữ ở bất kỳ trang nào mà không bị mất bộ lọc hay tham số đang dùng. |  |

## 2.3 Yêu cầu chức năng/phi chức năng của ứng dụng

### 2.3.1 Yêu cầu chức năng

Khách vãng lai:
- Tìm kiếm và lọc tour
- Quản lý giỏ hàng
- Đăng ký tài khoản
- Đăng nhập, đăng xuất
- Gửi liên hệ

Khách hàng (kế thừa toàn bộ chức năng của khách vãng lai):
- Đặt tour
- Xem lịch sử đơn đặt
- Cập nhật hồ sơ cá nhân
- Đổi mật khẩu
- Áp dụng mã giảm giá
- Đánh giá tour đã đi

Quản trị viên:
- Quản lý danh mục tour
- Quản lý tour và ảnh
- Quản lý đợt khởi hành
- Quản lý đơn đặt tour
- Quản lý tài khoản
- Xem thống kê doanh thu
- Quản lý mã khuyến mãi
- Kiểm duyệt đánh giá
- Duyệt liên hệ

### 2.3.2 Yêu cầu phi chức năng

Hiệu năng: các trang danh sách tour phải có phân trang, không nạp toàn bộ dữ liệu một lần; thao tác thêm vào giỏ hàng và kiểm tra mã giảm giá phải phản hồi qua AJAX, không tải lại toàn trang.

Giao diện (UI/UX): giao diện tối giản theo bảng màu "Ngọc bích & Đất nung", dùng phông Playfair Display cho tiêu đề và Be Vietnam Pro cho phần thân (chọn có chủ đích để hiển thị đúng dấu tiếng Việt), hỗ trợ hiển thị tốt trên cả máy tính lẫn điện thoại.

Tính đúng đắn: số chỗ còn trống và tổng tiền của đơn phải luôn khớp với các dòng chi tiết bên dưới; không được xảy ra tình trạng số chỗ còn trống âm dù nhiều người đặt cùng lúc.

Bảo mật: mật khẩu người dùng được băm bằng BCrypt; mọi form và API thay đổi dữ liệu được bảo vệ CSRF; khu vực quản trị và các trang tài khoản cá nhân được phân quyền chặt chẽ theo vai trò và theo quyền sở hữu dữ liệu.

Đa ngôn ngữ: toàn bộ câu chữ hiển thị hỗ trợ song song tiếng Việt và tiếng Anh, đổi ngôn ngữ giữ nguyên trang và tham số đang xem.

## 2.4 Sơ đồ phân cấp chức năng của ứng dụng

[[HINH: so-do-phan-cap | Sơ đồ phân cấp chức năng]]

Sơ đồ phân cấp chức năng chia hệ thống thành ba khối chính tương ứng ba nhóm tác nhân: khối chức năng công khai (xem trang chủ, tìm kiếm tour, xem chi tiết tour, giỏ hàng, đăng ký/đăng nhập), khối chức năng khách hàng (đặt tour, lịch sử đơn, hồ sơ cá nhân, đổi mật khẩu, mã giảm giá, đánh giá) kế thừa toàn bộ khối công khai, và khối chức năng quản trị (danh mục, tour, đợt khởi hành, đơn đặt, tài khoản, khuyến mãi, thống kê doanh thu).

## 2.5 Các chức năng chính cho ứng dụng (Mục tiêu của ứng dụng)

Mục tiêu cốt lõi của ứng dụng là giải quyết bài toán "đặt tour trực tuyến chính xác về số chỗ và minh bạch về giá". Các chức năng chính bao gồm:

Tìm kiếm và đặt tour trực tuyến: cho phép khách tìm tour theo nhiều tiêu chí (kể cả gõ không dấu), xem đầy đủ các đợt khởi hành kèm giá và số chỗ còn trống theo thời gian thực, và hoàn tất đặt tour chỉ qua vài bước với giỏ hàng, áp mã giảm giá và xác nhận.

Kiểm soát chỗ ngồi theo thời gian thực: mỗi đợt khởi hành có số chỗ độc lập, được trừ ngay trong giao dịch ghi đơn và có khoá lạc quan chống bán quá chỗ khi nhiều người đặt cùng lúc.

Quản lý vòng đời đơn đặt tour: từ chờ xác nhận đến hoàn thành hoặc huỷ, có nhật ký lịch sử đổi trạng thái, huỷ đơn thì tự động hoàn lại chỗ.

Khu vực quản trị toàn diện: quản lý danh mục, tour, ảnh, đợt khởi hành, đơn đặt, tài khoản, khuyến mãi và xem báo cáo doanh thu trực quan, với các quy tắc chặn xoá dữ liệu bảo vệ tính toàn vẹn nghiệp vụ.

## 2.6 Mô tả tổng quan ứng dụng

### 2.6.1 Mô hình Use case

[[HINH: 01. Biểu đồ Use Case tổng quát]]

(Hình 2.2 đặt trên một trang ngang để chữ đủ lớn; ảnh do `docs/report/diagrams/src/UseCase_TongQuat.py` vẽ và được `check_layout.py` kiểm tra: không đường nào xuyên oval, không nhãn nào đè lên hình hay đường.)

Quan hệ chính trên biểu đồ use case: tác nhân **Khách hàng** kế thừa (generalization) toàn bộ use case của **Khách vãng lai**, **Quản trị viên** kế thừa **Khách hàng**; use case **UC006 Đặt tour** có hai use case mở rộng («extend») là **UC007 Áp dụng mã giảm giá** và **UC008 Huỷ đơn đặt tour**; **UC009 Xem lịch sử đơn đặt** được mở rộng bởi **UC010 Đánh giá tour đã đi**; **Đăng xuất** là use case mở rộng của **UC005 Đăng nhập**, còn mọi chức năng cần đăng nhập đều «include» UC005; **UC004 Quản lý giỏ hàng** và các use case quản lý của quản trị viên có các use case mở rộng Thêm, Sửa, Xóa (riêng **UC016 Quản lý đơn đặt tour** chỉ có «extend» Sửa vì hệ thống không tạo hay xoá đơn thủ công).

### 2.6.2 Danh sách các tác nhân và mô tả

| Tác nhân | Mô tả tác nhân | Ghi chú |
|---|---|---|
| Khách vãng lai (AC01) | Người dùng chưa đăng nhập. Được xem trang chủ, xem giới thiệu, tìm kiếm/xem chi tiết tour, dùng giỏ hàng (giỏ hàng nằm trong phiên làm việc của trình duyệt), đăng ký tài khoản, đăng nhập, gửi liên hệ. |  |
| Khách hàng (AC02) | Tài khoản mang vai trò `CUSTOMER`. Kế thừa toàn bộ chức năng của Khách vãng lai, có thêm quyền đặt tour trực tuyến, xem lịch sử đơn, cập nhật hồ sơ, đổi mật khẩu, áp mã giảm giá, đánh giá tour đã đi. |  |
| Quản trị viên (AC03) | Tài khoản mang vai trò `ADMIN`. Có toàn quyền trên khu vực quản trị: quản lý danh mục, tour, đợt khởi hành, đơn đặt, tài khoản, khuyến mãi, kiểm duyệt đánh giá, duyệt liên hệ và xem thống kê doanh thu. |  |

### 2.6.3 Danh sách Use case và mô tả

| ID | Tên Use case | Mô tả ngắn gọn Use case | Chức năng | Ghi chú |
|---|---|---|---|---|
| UC001 | Tìm kiếm và lọc tour | Tìm tour theo từ khoá (kể cả không dấu), lọc theo danh mục, điểm đến, khoảng giá, sắp xếp; có thể tìm bằng AI; kết quả có phân trang. | Tìm kiếm tour |  |
| UC002 | Gửi liên hệ | Gửi câu hỏi/yêu cầu tư vấn qua biểu mẫu công khai, có thể gắn kèm một tour cụ thể. | Liên hệ |  |
| UC003 | Đăng ký tài khoản | Tạo tài khoản khách hàng mới với email và mật khẩu; mật khẩu được băm trước khi lưu; gửi email chào mừng. | Đăng ký |  |
| UC004 | Quản lý giỏ hàng | Thêm, sửa số lượng, xoá dòng, hoặc xoá toàn bộ giỏ hàng. Giỏ hàng nằm trong phiên làm việc, không lưu vào cơ sở dữ liệu. | Giỏ hàng | Thêm, Sửa, Xóa là các use case mở rộng («extend») của Quản lý giỏ hàng. |
| UC005 | Đăng nhập | Xác thực tài khoản để truy cập các chức năng dành riêng cho khách hàng hoặc quản trị viên; giỏ hàng được giữ nguyên qua bước đăng nhập. Đăng xuất là use case mở rộng («extend») của Đăng nhập. | Đăng nhập, đăng xuất | Các use case cần đăng nhập đều «include» Đăng nhập. |
| UC006 | Đặt tour | Từ giỏ hàng, xác nhận thông tin liên hệ và hành khách, ghi đơn đặt tour và trừ số chỗ trong một giao dịch duy nhất. | Đặt tour | «include» UC005; «extend»: UC007, UC008. |
| UC007 | Áp dụng mã giảm giá | Kiểm tra và áp một mã giảm giá vào giỏ hàng đang thanh toán, hiển thị ngay số tiền phải trả sau giảm. | Mã giảm giá | Use case mở rộng («extend») của UC006. |
| UC008 | Huỷ đơn đặt tour | Khách hàng tự huỷ đơn của mình theo chính sách thời gian còn lại tới ngày khởi hành; đơn đã có khoản thanh toán được hoàn tiền theo tỉ lệ tương ứng. | Huỷ đơn | Use case mở rộng («extend») của UC006; thực hiện từ trang chi tiết đơn. |
| UC009 | Xem lịch sử đơn đặt | Xem danh sách và chi tiết các đơn đã đặt của chính tài khoản đang đăng nhập. | Lịch sử đơn | «include» UC005. |
| UC010 | Đánh giá tour đã đi | Khách hàng đã có đơn hoàn thành cho một tour được viết đánh giá kèm số sao cho tour đó; đánh giá lưu ở trạng thái chờ duyệt. | Đánh giá tour | Use case mở rộng («extend») của UC009. |
| UC011 | Cập nhật hồ sơ cá nhân | Sửa họ tên, email, số điện thoại, địa chỉ của tài khoản đang đăng nhập. | Hồ sơ cá nhân | «include» UC005. |
| UC012 | Đổi mật khẩu | Đổi mật khẩu, yêu cầu xác nhận mật khẩu hiện tại. | Đổi mật khẩu | «include» UC005. |
| UC013 | Quản lý danh mục tour | Quản lý các danh mục tour: thêm mới, sửa thông tin, xoá danh mục (chặn xoá và gợi ý phương án thay thế nếu danh mục còn tour). | Quản lý danh mục | Use case đại diện cho mẫu thao tác Thêm - Sửa - Xoá của các chức năng quản lý khác (UC014, UC015, UC017, UC018); đặc tả chi tiết và sơ đồ ở mục 2.7.13. |
| UC014 | Quản lý tour | Quản lý các tour của hệ thống: thông tin, lịch trình, ảnh đại diện và thư viện ảnh, trạng thái mở bán. | Quản lý tour | Không đặc tả riêng; thao tác theo cùng mẫu với UC013. |
| UC015 | Quản lý lịch khởi hành | Quản lý các đợt khởi hành của một tour: ngày đi/về, giá người lớn/trẻ em và số chỗ. | Quản lý lịch khởi hành | Không đặc tả riêng; thao tác theo cùng mẫu với UC013. |
| UC016 | Quản lý đơn đặt tour | Xem danh sách và chi tiết đơn, đổi trạng thái đơn (kể cả huỷ đơn kèm hoàn lại số chỗ), sửa số lượng khách của một dòng, ghi nhận thanh toán. | Quản lý đơn đặt | Hệ thống không có thêm hoặc xoá đơn; use case mở rộng («extend») là Sửa. |
| UC017 | Quản lý tài khoản | Quản lý các tài khoản người dùng của hệ thống: thông tin, vai trò và trạng thái hoạt động. | Quản lý tài khoản | Không đặc tả riêng; thao tác theo cùng mẫu với UC013. |
| UC018 | Quản lý mã khuyến mãi | Quản lý các mã khuyến mãi: loại giảm giá, thời gian hiệu lực, số lượt dùng, xem lịch sử lượt dùng. | Mã khuyến mãi | Không đặc tả riêng; thao tác theo cùng mẫu với UC013. |
| UC019 | Xem thống kê doanh thu | Xem biểu đồ doanh thu theo tháng và các số liệu tổng quan trên trang chủ khu quản trị. | Thống kê doanh thu |  |
| UC020 | Kiểm duyệt đánh giá | Xem danh sách đánh giá theo trạng thái, duyệt/bỏ duyệt, trả lời, hoặc xoá một đánh giá trước khi nó được phép hiển thị công khai. | Kiểm duyệt đánh giá |  |
| UC021 | Duyệt liên hệ | Xem danh sách liên hệ theo trạng thái, ghi chú trả lời và đổi trạng thái Mới/Đang xử lý/Đã xử lý/Thư rác. | Duyệt liên hệ |  |

**Tổng: 21 use case (UC001-UC021).**

Ghi chú: UC014, UC015, UC017 và UC018 chỉ được liệt kê trong bảng trên (không đặc tả riêng); UC013 Quản lý danh mục tour là use case đại diện, có đặc tả chi tiết và ba cặp sơ đồ (Thêm, Sửa, Xoá). Các use case còn lại đều đã có giao diện hoàn chỉnh và chạy được thật. Bảng ánh xạ sang mã nội bộ UC001-UC026 của `SPEC_CHUNG.md` nằm ở mục 15 của tệp đó.

### 2.6.4 Các điều kiện phụ thuộc

Để ứng dụng hoạt động ổn định, hệ thống cần đáp ứng các điều kiện sau:

- Môi trường chạy: máy chủ phải cài đặt Java phiên bản 21 trở lên.
- Cơ sở dữ liệu: hệ quản trị cơ sở dữ liệu Microsoft SQL Server phải được cài đặt và đang hoạt động, có tạo sẵn cơ sở dữ liệu dành riêng cho hệ thống với bảng mã hỗ trợ tiếng Việt.
- Kết nối mạng: cần thiết nếu triển khai theo mô hình máy chủ - máy khách (Client-Server) cho nhiều người dùng truy cập cùng lúc.
- Thư viện phụ thuộc: các thư viện của dự án phải được cài đặt đầy đủ trước khi biên dịch và chạy chương trình.
- Thư mục lưu trữ: cần có một thư mục còn trống và có quyền ghi để lưu ảnh tour do quản trị viên tải lên.

## 2.7 Đặc tả các yêu cầu chức năng

### 2.7.1 UC001. Tìm kiếm và lọc tour

>>USECASE
ID: UC001
NAME: Tìm kiếm và lọc tour
PURPOSE: Giúp người dùng nhanh chóng tìm ra tour phù hợp với nhu cầu qua từ khoá và các tiêu chí lọc, kể cả khi gõ tiếng Việt không dấu.
DESC: Người dùng vào trang danh sách tour, nhập từ khoá và/hoặc chọn bộ lọc (danh mục, điểm đến, khoảng giá) cùng cách sắp xếp (mới nhất, giá tăng dần, giá giảm dần, xem nhiều); hệ thống so khớp trên dữ liệu đã được chuẩn hoá bỏ dấu và trả về danh sách tour đang mở bán có phân trang, giữ nguyên bộ lọc khi chuyển trang hoặc đổi ngôn ngữ. Nếu người dùng bật công tắc “Tìm bằng AI”, hệ thống thử xếp kết quả theo độ phù hợp với mô tả bằng lời trước, không được thì quay về tìm theo từ khoá.
ACTORS: Khách vãng lai (Khách hàng kế thừa)
PRE: Không bắt buộc đăng nhập; đã có ít nhất một tour đang mở bán.
POST: Danh sách tour khớp với từ khoá và bộ lọc được hiển thị theo trang, kèm số tour tìm thấy; nếu không có kết quả thì hiển thị thông báo “Không có tour nào khớp với điều kiện tìm kiếm. Thử bỏ bớt bộ lọc xem sao.”
MAIN:
1|A|Mở trang “Tất cả tour” từ thanh điều hướng, hoặc chọn một danh mục để xem các tour của danh mục đó
2|S|Hiển thị biểu mẫu tìm kiếm/lọc và trang đầu tiên của danh sách tour đang mở bán, sắp theo tour mới nhất
3|A|Nhập từ khoá (ví dụ “da lat”) và/hoặc chọn danh mục, điểm đến, khoảng giá, cách sắp xếp, bấm “Áp dụng”
4|S|Chuẩn hoá từ khoá về dạng không dấu, chữ thường; so khớp với dữ liệu đã chuẩn hoá sẵn của từng tour, áp các bộ lọc và cách sắp xếp
5|S|Hiển thị danh sách tour khớp điều kiện, kèm số lượng kết quả (“Tìm thấy N tour”) và điều khiển phân trang
6|A|Chuyển sang trang kết quả kế tiếp hoặc đổi ngôn ngữ hiển thị
7|S|Giữ nguyên toàn bộ từ khoá và bộ lọc đang áp dụng khi hiển thị trang mới
ALT:
4.1|S|Nếu không có tour nào khớp điều kiện thì hiển thị thông báo “Không có tour nào khớp với điều kiện tìm kiếm. Thử bỏ bớt bộ lọc xem sao.”
3.1|A|Nếu bật công tắc “Tìm bằng AI” và nhập mô tả bằng lời (ví dụ “biển yên tĩnh cho gia đình dưới 5 triệu”)
4.2|S|Hệ thống xếp kết quả theo độ phù hợp với mô tả, có hiểu cả mức giá nêu trong câu
4.3|S|Nếu tính năng AI chưa được cấu hình thì công tắc bị vô hiệu hoá kèm ghi chú “AI chưa được cấu hình - đang dùng tìm theo từ khoá”; nếu AI gặp sự cố hoặc không tìm được kết quả phù hợp thì hệ thống tự quay về tìm theo từ khoá và báo rõ điều đó trên trang kết quả
DIAGRAMS:
AC-UC001. Biểu đồ hoạt động - Tìm kiếm và lọc tour
SQ-UC001. Biểu đồ tuần tự - Tìm kiếm và lọc tour
>>END

### 2.7.2 UC002. Gửi liên hệ

>>USECASE
ID: UC002
NAME: Gửi liên hệ
PURPOSE: Cho phép bất kỳ ai - kể cả chưa có tài khoản - gửi câu hỏi hoặc yêu cầu tư vấn tới bộ phận chăm sóc khách hàng, có thể gắn kèm một tour cụ thể đang quan tâm.
DESC: Khách có thể mở biểu mẫu liên hệ trực tiếp, hoặc bấm “Liên hệ về tour này” từ trang chi tiết một tour đang quan tâm - khi đó tour tương ứng được gắn kèm tự động vào liên hệ. Liên hệ gửi lên luôn ở trạng thái “Mới” và không đòi hỏi phải có tài khoản. Liên hệ được quản trị viên xử lý ở chức năng Duyệt liên hệ (UC021).
ACTORS: Khách vãng lai (Khách hàng kế thừa)
PRE: Không bắt buộc đăng nhập.
POST: Một liên hệ mới được ghi nhận ở trạng thái “Mới”; nếu gửi từ trang chi tiết tour thì tour đang hỏi được gắn kèm theo.
MAIN:
1|A|Mở trang “Liên hệ” từ thanh điều hướng, hoặc bấm “Liên hệ về tour này” từ trang chi tiết một tour
2|S|Hiển thị biểu mẫu liên hệ; nếu mở từ một tour cụ thể thì dòng “Đang hỏi về tour …” hiện sẵn phía trên biểu mẫu
3|A|Nhập họ tên, email, số điện thoại (tuỳ chọn), tiêu đề, nội dung, bấm “Gửi liên hệ”
4|S|Kiểm tra dữ liệu nhập: họ tên, email, tiêu đề, nội dung bắt buộc; email đúng định dạng; độ dài không vượt giới hạn (họ tên 100, tiêu đề 200, nội dung 2000 ký tự)
5|S|Ghi nhận liên hệ mới ở trạng thái “Mới”, gắn kèm tour đang hỏi nếu có
6|S|Hiển thị lại trang liên hệ kèm thông báo “Cảm ơn bạn đã liên hệ! Chúng tôi sẽ phản hồi sớm nhất có thể.”
ALT:
2.1|S|Nếu tour được nhắc tới không còn tồn tại thì vẫn hiển thị biểu mẫu bình thường, không báo lỗi và không gắn tour
4.1|S|Nếu dữ liệu nhập thiếu hoặc sai định dạng thì trả lại biểu mẫu kèm thông báo lỗi tại đúng ô nhập, giữ nguyên nội dung đã nhập và tour đang hỏi (nếu có)
DIAGRAMS:
AC-UC002. Biểu đồ hoạt động - Gửi liên hệ
SQ-UC002. Biểu đồ tuần tự - Gửi liên hệ
>>END

### 2.7.3 UC003. Đăng ký tài khoản

>>USECASE
ID: UC003
NAME: Đăng ký tài khoản
PURPOSE: Cho phép khách vãng lai tạo tài khoản để sử dụng các chức năng dành riêng cho khách hàng (đặt tour, xem lịch sử đơn...).
DESC: Người dùng điền biểu mẫu đăng ký gồm họ tên, email, mật khẩu, nhập lại mật khẩu, số điện thoại và địa chỉ (tuỳ chọn); hệ thống kiểm tra đồng thời mọi ràng buộc, mã hoá mật khẩu rồi tạo tài khoản mới với vai trò khách hàng và gửi email chào mừng.
ACTORS: Khách vãng lai
PRE: Người dùng chưa đăng nhập.
POST: Tài khoản mới được tạo với vai trò khách hàng, mật khẩu được mã hoá an toàn trước khi lưu; một email chào mừng được gửi tới địa chỉ đăng ký; người dùng được chuyển tới trang đăng nhập.
MAIN:
1|A|Bấm “Đăng ký” từ thanh điều hướng
2|S|Hiển thị biểu mẫu đăng ký
3|A|Nhập họ tên, email, mật khẩu, nhập lại mật khẩu, số điện thoại, địa chỉ (tuỳ chọn), bấm “Đăng ký”
4|S|Kiểm tra cùng lúc mọi ràng buộc: các trường bắt buộc không rỗng, email đúng định dạng và chưa được đăng ký, mật khẩu từ 6 đến 50 ký tự, mật khẩu và nhập lại mật khẩu trùng khớp, số điện thoại đúng định dạng Việt Nam
5|S|Mã hoá mật khẩu, lưu tài khoản mới với vai trò khách hàng ở trạng thái đang hoạt động
6|S|Sau khi tài khoản đã được lưu, gửi một email chào mừng tới địa chỉ vừa đăng ký
7|S|Chuyển tới trang đăng nhập kèm thông báo “Đăng ký thành công. Mời bạn đăng nhập.”
ALT:
4.1|S|Nếu có ràng buộc không đạt thì trả lại biểu mẫu, báo lỗi cùng lúc tại tất cả các ô sai và giữ lại các thông tin đã nhập; không tạo tài khoản
4.2|S|Nếu email đã được đăng ký thì báo lỗi tại đúng ô email
4.3|S|Nếu mật khẩu và nhập lại mật khẩu không trùng khớp thì báo lỗi tại ô nhập lại mật khẩu
4.4|S|Nếu số điện thoại sai định dạng (không bắt đầu bằng số 0, hoặc không đủ 10-11 chữ số) thì báo lỗi tại ô số điện thoại
6.1|S|Nếu gửi email chào mừng không thành công (ví dụ do sự cố mạng) thì tài khoản vẫn được tạo bình thường, lỗi gửi thư chỉ được ghi nhận nội bộ, không ảnh hưởng và không hiển thị cho người dùng
DIAGRAMS:
AC-UC003. Biểu đồ hoạt động - Đăng ký tài khoản
SQ-UC003. Biểu đồ tuần tự - Đăng ký tài khoản
>>END

### 2.7.4 UC004. Quản lý giỏ hàng

>>USECASE
ID: UC004
NAME: Quản lý giỏ hàng
PURPOSE: Cho phép người dùng gom nhiều lịch khởi hành muốn đặt vào một nơi trước khi thanh toán, không đòi hỏi phải đăng nhập.
DESC: Giỏ hàng được giữ tạm trong phiên làm việc của người dùng, không lưu trữ lâu dài trong cơ sở dữ liệu. Người dùng thêm, sửa số lượng hoặc xoá từng dòng, hoặc xoá toàn bộ giỏ; mỗi dòng ứng với một lịch khởi hành. Thao tác thêm vào giỏ được cập nhật ngay trên trang đang xem, không tải lại trang; các thao tác sửa, xoá thực hiện ở trang giỏ hàng.
ACTORS: Khách vãng lai (Khách hàng kế thừa)
PRE: Không bắt buộc đăng nhập.
POST: Giỏ hàng phản ánh đúng các lịch khởi hành và số lượng khách người dùng đã chọn; tổng tiền được tính lại mỗi khi giỏ thay đổi.
MAIN:
1|A|Bấm “Thêm vào giỏ” ở trang chi tiết tour hoặc trang danh sách tour (chọn lịch khởi hành, số người lớn/trẻ em)
2|S|Kiểm tra số khách (có ít nhất 1 người lớn, tối đa 30 khách mỗi dòng), lịch khởi hành còn mở bán và còn đủ chỗ
3|S|Nếu lịch khởi hành đã có sẵn trong giỏ thì cộng dồn số khách vào dòng cũ, ngược lại thêm một dòng mới
4|S|Cập nhật số lượng trên biểu tượng giỏ hàng và hiện thông báo thành công ngay trên trang đang xem, không tải lại trang
5|A|Mở trang giỏ hàng, xem các dòng kèm ảnh, tên tour, ngày đi, số khách, thành tiền từng dòng và tổng tiền
6|A|Sửa số người lớn/trẻ em của một dòng rồi bấm “Cập nhật”, hoặc bấm “Xoá” một dòng, hoặc bấm “Xoá toàn bộ giỏ”
7|S|Ghi lại thay đổi trong phiên làm việc (khi sửa số khách thì kiểm tra lại số khách và số chỗ); nếu tổng số khách của dòng về 0 thì tự xoá dòng đó khỏi giỏ
8|S|Tải lại trang giỏ hàng với số liệu đã cập nhật kèm thông báo kết quả
ALT:
2.1|S|Nếu số khách không hợp lệ (không có người lớn, vượt 30 khách), lịch khởi hành đã đóng hoặc quá hạn chót đặt, hoặc số chỗ còn trống không đủ cho tổng số khách (tính cả số khách đã có trong giỏ) thì từ chối, giữ nguyên giỏ và hiện thông báo lỗi ngay trên trang đang xem (ví dụ “Không đủ chỗ, lịch khởi hành này chỉ còn N chỗ trống.”)
2.2|S|Nếu người dùng thêm trực tiếp từ một tour mà không chọn lịch khởi hành thì hệ thống tự chọn lịch khởi hành gần nhất còn đủ chỗ; nếu tour chưa có lịch nào còn chỗ thì từ chối kèm thông báo
7.1|S|Nếu số khách mới của dòng không hợp lệ hoặc vượt số chỗ còn trống thì từ chối, giữ nguyên dòng cũ và hiện thông báo lỗi ở trang giỏ hàng
DIAGRAMS:
AC-UC004. Biểu đồ hoạt động - Quản lý giỏ hàng
SQ-UC004. Biểu đồ tuần tự - Quản lý giỏ hàng
>>END

### 2.7.5 UC005. Đăng nhập

>>USECASE
ID: UC005
NAME: Đăng nhập
PURPOSE: Xác thực người dùng để truy cập các chức năng dành cho khách hàng hoặc quản trị viên, đồng thời giữ nguyên giỏ hàng đang có khi khách đăng nhập giữa chừng lúc đặt tour.
DESC: Người dùng nhập email và mật khẩu; hệ thống xác thực thông tin và cấp quyền truy cập theo đúng vai trò của tài khoản. Đăng xuất là use case mở rộng («extend») của Đăng nhập: khi đăng xuất, phiên làm việc kết thúc và quyền truy cập bị thu hồi. Các chức năng cần đăng nhập đều «include» use case này.
ACTORS: Khách vãng lai (đăng nhập); Khách hàng, Quản trị viên (đăng xuất)
PRE: Đã có tài khoản hợp lệ và đang hoạt động trong hệ thống.
POST: Người dùng được xác thực, chuyển tới trang tương ứng vai trò (trang người dùng đã yêu cầu trước đó hoặc trang chủ với khách hàng; trang tổng quan với quản trị viên); giỏ hàng đang có trước khi đăng nhập vẫn còn nguyên. Sau khi đăng xuất, người dùng trở về trạng thái khách vãng lai.
MAIN:
1|A|Bấm “Đăng nhập” trên thanh điều hướng (hoặc được chuyển tới đây khi mở chức năng yêu cầu đăng nhập)
2|S|Hiển thị trang đăng nhập
3|A|Nhập email và mật khẩu (có thể chọn “Ghi nhớ đăng nhập”), bấm “Đăng nhập”
4|S|Tìm tài khoản theo email, so khớp mật khẩu đã mã hoá và kiểm tra tài khoản còn hoạt động
5|S|Tạo phiên đăng nhập mới, giữ nguyên giỏ hàng hiện có của người dùng (nếu chọn “Ghi nhớ đăng nhập” thì đăng nhập được ghi nhớ trong 7 ngày)
6|S|Điều hướng theo vai trò: quản trị viên không có trang nào đang chờ thì về trang tổng quan khu quản trị; các trường hợp còn lại về trang đã yêu cầu trước đó hoặc trang chủ
7|A|[«extend» Đăng xuất] Bấm “Đăng xuất” ở menu tài khoản
8|S|Kết thúc phiên làm việc (giỏ hàng trong phiên cũng không còn), xoá thông tin đăng nhập trên trình duyệt, chuyển về trang chủ kèm thông báo “Bạn đã đăng xuất.”
ALT:
4.1|S|Nếu email không tồn tại hoặc mật khẩu sai thì hiển thị lại trang đăng nhập kèm thông báo “Email hoặc mật khẩu không đúng.”
4.2|S|Nếu tài khoản đã bị quản trị viên vô hiệu hoá thì từ chối đăng nhập; hệ thống hiển thị cùng thông báo như ở 4.1, không báo riêng lý do tài khoản bị vô hiệu hoá
DIAGRAMS:
AC-UC005. Biểu đồ hoạt động - Đăng nhập
SQ-UC005. Biểu đồ tuần tự - Đăng nhập
>>END

### 2.7.6 UC006. Đặt tour

>>USECASE
ID: UC006
NAME: Đặt tour
PURPOSE: Chức năng trung tâm của hệ thống: chuyển giỏ hàng thành một đơn đặt tour chính thức, đảm bảo số chỗ và số tiền chính xác tuyệt đối.
DESC: Khách hàng đã đăng nhập [«include» UC005 Đăng nhập], từ giỏ hàng đi tới trang thanh toán, xác nhận thông tin liên hệ, nhập họ tên từng hành khách, có thể áp mã giảm giá [«extend» UC007], rồi xác nhận đặt. Toàn bộ việc kiểm tra chỗ, trừ chỗ, tính tiền và ghi nhận đơn được thực hiện trọn vẹn trong một lần xử lý duy nhất, không thể xảy ra tình trạng đơn được ghi nhận nửa chừng. Cùng lúc đó, hệ thống tự động khởi tạo một khoản thanh toán ở trạng thái chờ thanh toán và ghi nhận mốc đầu tiên trong dòng lịch sử thay đổi trạng thái của đơn. Sau khi đặt, khách có thể huỷ đơn theo chính sách (UC008).
ACTORS: Khách hàng
PRE: Khách đã đăng nhập, giỏ hàng không rỗng, các lịch khởi hành trong giỏ vẫn đang mở bán, còn đủ chỗ và còn cách ngày khởi hành tối thiểu 2 ngày (chưa quá hạn chót đặt tour) tại thời điểm xác nhận.
POST: Đơn đặt tour mới được ghi nhận với mã đơn duy nhất, trạng thái “Chờ xác nhận”; số chỗ còn trống của từng lịch khởi hành giảm đúng bằng số khách đã đặt; giỏ hàng được làm rỗng; nếu có mã giảm giá thì lượt dùng được ghi nhận; một mốc lịch sử trạng thái đầu tiên, một khoản thanh toán “Chờ thanh toán” và danh sách hành khách (người lớn/trẻ em) của từng dòng được ghi nhận cùng lúc; email xác nhận được gửi tới khách. Nếu sau 24 giờ kể từ lúc đặt mà đơn vẫn chưa có khoản thanh toán nào được xác nhận, hệ thống tự động huỷ đơn để trả lại chỗ cho người khác.
MAIN:
1|A|Từ trang giỏ hàng, bấm “Tiến hành đặt tour” [«include» UC005 Đăng nhập nếu chưa đăng nhập]
2|S|Hiển thị trang thanh toán: tóm tắt giỏ hàng, biểu mẫu thông tin liên hệ được điền sẵn từ hồ sơ, ô nhập họ tên từng hành khách (người lớn đầu tiên được điền sẵn tên và số điện thoại của người đặt), ô nhập mã giảm giá, lựa chọn hình thức thanh toán
3|A|Kiểm tra/sửa lại thông tin liên hệ, chọn hình thức thanh toán, nhập họ tên từng hành khách (người lớn/trẻ em) cho mỗi dòng đặt tour
4|A|Nhập mã giảm giá và bấm “Áp dụng” [«extend» UC007 Áp dụng mã giảm giá]
5|S|Kiểm tra mã, tính số tiền được giảm, hiển thị lại tổng tiền sau giảm giá ngay trên trang
6|A|Bấm “Xác nhận đặt tour”
7|S|Kiểm tra dữ liệu nhập: thông tin liên hệ đúng định dạng, danh sách hành khách khớp đúng số người lớn/trẻ em của từng dòng trong giỏ
8|S|Với từng lịch khởi hành trong giỏ: đọc lại số chỗ thật (khoá lại để không ai khác đặt chen), kiểm tra còn mở bán và chưa quá hạn chót đặt, kiểm tra đủ chỗ, kiểm tra tên từng hành khách (bắt buộc, ngày sinh không ở tương lai), rồi trừ số chỗ
9|S|Kiểm tra lại mã giảm giá (nếu có) theo tổng tiền thật của đơn, sinh mã đơn, tính lại tổng tiền và số tiền giảm giá
10|S|Ghi nhận đơn, các dòng chi tiết và danh sách hành khách, ghi lượt dùng mã giảm giá (nếu có), thêm mốc đầu tiên vào dòng lịch sử trạng thái (chuyển sang “Chờ xác nhận”), khởi tạo khoản thanh toán “Chờ thanh toán” đúng bằng tổng tiền phải trả
11|S|Làm rỗng giỏ hàng, gửi email xác nhận đơn tới khách rồi chuyển tới trang xác nhận thành công kèm mã đơn
ALT:
2.1|S|Nếu giỏ hàng đang trống thì không hiển thị trang thanh toán mà quay về trang giỏ hàng
7.1|S|Nếu thông tin liên hệ nhập sai định dạng, hoặc danh sách hành khách không còn khớp với giỏ hàng (giỏ vừa thay đổi), thì không ghi nhận gì, trả lại trang thanh toán kèm thông báo lỗi tại đúng ô nhập, giữ nguyên tên đã nhập và dựng lại các ô hành khách theo giỏ hiện tại
8.1|S|Nếu một lịch khởi hành trong giỏ không còn tồn tại, đã đóng, đã quá hạn chót đặt, tour đã ngừng bán hoặc không còn đủ chỗ (do người khác vừa đặt trước) thì không ghi nhận bất kỳ phần nào của đơn, quay về trang giỏ hàng kèm thông báo lỗi nêu tên tour để khách điều chỉnh hoặc xoá dòng đó
8.2|S|Nếu thiếu tên một hoặc nhiều hành khách, hoặc ngày sinh hành khách ở tương lai thì không ghi nhận bất kỳ phần nào của đơn, quay về trang giỏ hàng kèm thông báo lỗi tương ứng
9.1|S|Nếu mã giảm giá không còn hợp lệ tại thời điểm xác nhận (hết hạn, hết lượt, đã dùng, chưa đạt giá trị tối thiểu) thì không ghi nhận đơn, quay về trang giỏ hàng kèm thông báo lỗi tương ứng
11.1|S|Nếu gửi email xác nhận không thành công thì đơn vẫn được ghi nhận bình thường, lỗi chỉ được ghi nhận nội bộ
DIAGRAMS:
AC-UC006. Biểu đồ hoạt động - Đặt tour
SQ-UC006. Biểu đồ tuần tự - Đặt tour
>>END

### 2.7.7 UC007. Áp dụng mã giảm giá

>>USECASE
ID: UC007
NAME: Áp dụng mã giảm giá
PURPOSE: Cho phép khách hàng dùng thử một mã giảm giá ngay tại trang thanh toán và thấy ngay số tiền được giảm trước khi xác nhận đặt tour.
DESC: Đây là use case mở rộng («extend») của UC006 Đặt tour. Khách nhập mã, hệ thống kiểm tra các điều kiện áp dụng dựa trên giá trị giỏ hàng hiện tại (không tin số liệu do trình duyệt gửi lên) rồi hiển thị số tiền được giảm ngay trên trang thanh toán, không tải lại trang. Việc áp mã ở đây chỉ để xem trước; mã được kiểm tra lại và ghi nhận khi khách xác nhận đặt tour.
ACTORS: Khách hàng
PRE: Khách hàng đã đăng nhập, đang ở trang thanh toán với giỏ hàng không rỗng.
POST: Nếu mã hợp lệ, tên chương trình, số tiền được giảm và tổng tiền sau giảm giá được hiển thị trên trang; nếu không hợp lệ, hiển thị lý do bị từ chối và tổng tiền giữ nguyên.
MAIN:
1|A|Tại trang thanh toán, nhập mã giảm giá vào ô tương ứng, bấm “Áp dụng”
2|S|Đọc tổng tiền giỏ hàng hiện tại
3|S|Kiểm tra lần lượt: mã có tồn tại không; mã có đang bật, trong thời gian hiệu lực và còn lượt sử dụng chung không; tài khoản này còn lượt sử dụng riêng không; giỏ hàng có đạt giá trị tối thiểu không
4|S|Tính số tiền được giảm (theo phần trăm có áp trần, hoặc theo số tiền cố định)
5|S|Trả về tên chương trình, số tiền được giảm, tổng tiền trước và sau giảm giá
6|S|Trang thanh toán hiển thị số tiền giảm và tổng tiền đã trừ giảm giá, không tải lại trang
ALT:
1.1|S|Nếu ô mã để trống thì không kiểm tra, ẩn dòng giảm giá và giữ nguyên tổng tiền
3.1|S|Nếu mã không tồn tại thì từ chối áp dụng, thông báo “Mã “X” không tồn tại.”
3.2|S|Nếu mã đã bị tắt, chưa tới ngày bắt đầu hoặc đã quá ngày kết thúc thì từ chối áp dụng, thông báo “Mã “X” đã hết hạn hoặc chưa tới ngày áp dụng.”
3.3|S|Nếu mã đã hết lượt sử dụng chung thì từ chối áp dụng, thông báo “Mã “X” đã hết lượt sử dụng.”
3.4|S|Nếu tài khoản này đã dùng đủ số lượt cho phép của mã thì từ chối áp dụng, thông báo “Mỗi tài khoản chỉ được dùng mã “X” N lần.”
3.5|S|Nếu giỏ hàng chưa đạt giá trị tối thiểu thì từ chối áp dụng, thông báo nêu rõ giá trị tối thiểu cần đạt
DIAGRAMS:
AC-UC007. Biểu đồ hoạt động - Áp dụng mã giảm giá
SQ-UC007. Biểu đồ tuần tự - Áp dụng mã giảm giá
>>END

### 2.7.8 UC008. Huỷ đơn đặt tour

>>USECASE
ID: UC008
NAME: Huỷ đơn đặt tour
PURPOSE: Cho khách hàng tự huỷ đơn đặt tour của mình khi có nhu cầu thay đổi kế hoạch, không phải liên hệ tổng đài cho mọi trường hợp, đồng thời bảo vệ quyền lợi của công ty bằng chính sách hoàn tiền theo thời gian còn lại tới ngày khởi hành.
DESC: Đây là use case mở rộng («extend») của UC006 Đặt tour, được thực hiện từ trang chi tiết đơn của chính khách (xem UC009). Nếu đơn đang ở trạng thái “Chờ xác nhận” hoặc “Đã xác nhận”, trang chi tiết đơn hiện sẵn khối “Huỷ đơn” nêu tỉ lệ hoàn tiền và số tiền sẽ được hoàn (nếu có) theo số ngày còn lại tới ngày khởi hành sớm nhất của đơn. Khách bấm huỷ và xác nhận ở hộp thoại nêu rõ số tiền hoàn. Sau khi huỷ, số chỗ đã giữ được trả lại cho lịch khởi hành, lượt dùng mã giảm giá (nếu có) được trả lại, và một mốc mới được thêm vào dòng lịch sử trạng thái của đơn.
ACTORS: Khách hàng
PRE: Khách đã đăng nhập; đơn đang xem là đơn của chính khách; đơn đang ở trạng thái “Chờ xác nhận” hoặc “Đã xác nhận”.
POST: Đơn chuyển sang trạng thái “Đã huỷ”; số chỗ của lịch khởi hành tăng lại đúng bằng số khách của đơn; các khoản thanh toán đang chờ được đóng; nếu đơn đã có khoản thanh toán được xác nhận thì phát sinh thêm một khoản hoàn tiền theo đúng tỉ lệ của chính sách; lượt dùng mã giảm giá (nếu có) được trả lại; lịch sử trạng thái của đơn có thêm một mốc mới ghi lý do “Khách tự huỷ”.
MAIN:
1|A|Mở “Đơn của tôi”, chọn một đơn, mở trang chi tiết đơn
2|S|Kiểm tra quyền sở hữu, hiển thị đầy đủ thông tin đơn; nếu đơn còn được huỷ thì tính ngay số ngày còn lại tới ngày khởi hành sớm nhất và hiện khối “Huỷ đơn” kèm tỉ lệ hoàn tiền, số tiền sẽ được hoàn và nút “Huỷ đơn này”
3|A|Bấm “Huỷ đơn này”
4|S|Hiển thị hộp xác nhận “Bạn có chắc muốn huỷ đơn này? Số tiền sẽ được hoàn: …”
5|A|Xem lại số tiền sẽ hoàn, bấm đồng ý
6|S|Kiểm tra lại quyền sở hữu, đơn còn huỷ được và còn kịp thời hạn tự huỷ
7|S|Trả lại số chỗ cho lịch khởi hành, trả lại lượt dùng mã giảm giá (nếu có), đóng các khoản thanh toán đang chờ, chuyển đơn sang trạng thái “Đã huỷ”, thêm mốc mới vào dòng lịch sử trạng thái với lý do “Khách tự huỷ”
8|S|Nếu đơn đã có khoản thanh toán được xác nhận, ghi thêm một khoản hoàn tiền đúng theo tỉ lệ của chính sách (khoản đã thu vẫn giữ nguyên làm lịch sử, không bị xoá)
9|S|Hiển thị lại trang chi tiết đơn ở trạng thái “Đã huỷ” kèm thông báo “Đã huỷ đơn thành công.”
ALT:
2.1|S|Nếu ngày khởi hành sớm nhất còn cách hôm nay từ 7 ngày trở lên: số tiền hoàn bằng 100% khoản đã thu
2.2|S|Nếu ngày khởi hành sớm nhất còn cách hôm nay từ 3 đến 6 ngày: số tiền hoàn bằng 50% khoản đã thu
2.3|S|Nếu ngày khởi hành sớm nhất còn cách hôm nay dưới 3 ngày: không cho tự huỷ - không hiện nút “Huỷ đơn này”, thay bằng dòng hướng dẫn khách liên hệ tổng đài để được hỗ trợ
2.4|S|Nếu đơn đã ở trạng thái “Hoàn thành” hoặc “Đã huỷ” thì không hiển thị khối “Huỷ đơn”
2.5|S|Nếu đơn đang mở không phải của tài khoản đang đăng nhập thì từ chối truy cập, không tiết lộ thông tin của đơn
6.1|S|Nếu tại bước xác nhận đơn không còn huỷ được hoặc đã quá cận ngày khởi hành (ví dụ trang để mở quá lâu) thì không huỷ gì, quay lại trang chi tiết đơn kèm thông báo lỗi tương ứng
DIAGRAMS:
AC-UC008. Biểu đồ hoạt động - Huỷ đơn đặt tour
SQ-UC008. Biểu đồ tuần tự - Huỷ đơn đặt tour
>>END

### 2.7.9 UC009. Xem lịch sử đơn đặt

>>USECASE
ID: UC009
NAME: Xem lịch sử đơn đặt
PURPOSE: Cho phép khách hàng tự tra cứu lại các đơn đã đặt mà không cần liên hệ quản trị viên.
DESC: Khách hàng mở trang lịch sử đơn, xem danh sách các đơn của chính mình có phân trang, mới nhất trước; bấm vào một đơn để xem chi tiết. Hệ thống luôn kiểm tra quyền sở hữu của từng đơn trước khi hiển thị. Từ trang chi tiết đơn, khách có thể huỷ đơn (UC008) hoặc bấm vào tên tour để mở trang tour và đánh giá tour đã đi (UC010 «extend»).
ACTORS: Khách hàng
PRE: Khách hàng đã đăng nhập [«include» UC005].
POST: Danh sách hoặc chi tiết đơn của đúng tài khoản đang đăng nhập được hiển thị.
MAIN:
1|A|Mở menu tài khoản, chọn “Đơn của tôi”
2|S|Lấy danh sách đơn của đúng tài khoản đang đăng nhập, có phân trang, sắp xếp theo ngày đặt gần nhất
3|S|Hiển thị danh sách kèm mã đơn, ngày đặt, tổng tiền, trạng thái
4|A|Bấm vào một đơn để xem chi tiết
5|S|Kiểm tra đơn được yêu cầu có thuộc về tài khoản đang đăng nhập không (quản trị viên được xem mọi đơn)
6|S|Hiển thị chi tiết đơn: thông tin liên hệ đã dùng lúc đặt, hình thức thanh toán, danh sách các dòng lịch khởi hành kèm số khách và thành tiền, mã giảm giá đã áp (nếu có), danh sách hành khách, lịch sử đổi trạng thái và các khoản thanh toán; nếu đơn đang ở trạng thái còn được tự huỷ thì hiện thêm khối “Huỷ đơn” dẫn sang chức năng huỷ đơn đặt tour (UC008)
ALT:
5.1|S|Nếu đơn được yêu cầu không thuộc tài khoản đang đăng nhập thì từ chối truy cập, không tiết lộ thông tin của đơn
5.2|S|Nếu mã đơn không tồn tại thì hiển thị trang thông báo không tìm thấy đơn
DIAGRAMS:
AC-UC009. Biểu đồ hoạt động - Xem lịch sử đơn đặt
SQ-UC009. Biểu đồ tuần tự - Xem lịch sử đơn đặt
>>END

### 2.7.10 UC010. Đánh giá tour đã đi

>>USECASE
ID: UC010
NAME: Đánh giá tour đã đi
PURPOSE: Thu thập phản hồi thật của khách hàng đã đi tour, giúp khách hàng sau tham khảo và quản trị viên cải thiện dịch vụ.
DESC: Đây là use case mở rộng («extend») của UC009 Xem lịch sử đơn đặt: từ chi tiết một đơn đã hoàn thành, khách bấm vào tên tour để mở trang chi tiết tour. Tại đó hệ thống hiển thị các đánh giá đã được duyệt, kèm điểm trung bình và số lượng đánh giá đã duyệt. Nếu tài khoản đang xem đủ điều kiện (đã đăng nhập, có đơn đã hoàn thành chứa tour này, và chưa từng đánh giá), biểu mẫu đánh giá sẽ hiện ra. Đánh giá gửi lên được ghi nhận ở trạng thái chờ duyệt, chưa hiển thị công khai cho tới khi quản trị viên duyệt ở chức năng Kiểm duyệt đánh giá (UC020). Khách luôn thấy đánh giá của chính mình ngay trên trang tour kèm nhãn “Chờ duyệt” cho tới khi được xử lý.
ACTORS: Khách hàng
PRE: Khách hàng đã đăng nhập [«include» UC005]; đã có ít nhất một đơn đặt tour ở trạng thái “Hoàn thành” có chứa tour này; chưa từng đánh giá tour này.
POST: Đánh giá mới được ghi nhận ở trạng thái chờ duyệt, gắn kèm bằng chứng là đơn đã hoàn thành tương ứng (sẽ hiện nhãn “Đã xác thực”); đánh giá chưa hiển thị công khai và chưa được tính vào điểm trung bình của tour cho tới khi được quản trị viên duyệt. Khách thấy đánh giá của chính mình ở trang tour kèm nhãn “Chờ duyệt”.
MAIN:
1|A|Từ chi tiết đơn đã hoàn thành, bấm vào tên tour để mở trang chi tiết tour và cuộn tới khối đánh giá
2|S|Hiển thị các đánh giá đã duyệt, điểm trung bình; kiểm tra tài khoản đang xem có đủ điều kiện đánh giá hay không và nếu đủ thì hiện biểu mẫu đánh giá
3|A|Chọn số sao (1-5, mặc định 5), nhập tiêu đề (tuỳ chọn) và nội dung nhận xét, bấm gửi đánh giá
4|S|Kiểm tra dữ liệu nhập: số sao từ 1 đến 5, nội dung không được để trống (tối đa 1000 ký tự), tiêu đề tối đa 200 ký tự
5|S|Kiểm tra lại điều kiện đánh giá trước khi ghi nhận: chưa từng đánh giá tour này, và đã có đơn hoàn thành chứa tour này
6|S|Ghi nhận đánh giá mới ở trạng thái chờ duyệt, gắn kèm đơn đã hoàn thành làm bằng chứng
7|S|Hiển thị lại trang chi tiết tour kèm thông báo cảm ơn “Cảm ơn bạn! Đánh giá của bạn đã được đăng.”; đánh giá của khách hiện kèm nhãn “Chờ duyệt”, chưa xuất hiện với người khác và điểm trung bình của tour chưa đổi
ALT:
2.1|S|Nếu tài khoản chưa đăng nhập, chưa có đơn hoàn thành chứa tour này, hoặc đã đánh giá rồi thì không hiện biểu mẫu đánh giá, chỉ hiện danh sách đánh giá đã duyệt (và đánh giá của chính khách nếu có)
4.1|S|Nếu dữ liệu nhập không hợp lệ (số sao ngoài khoảng 1-5, nội dung trống hoặc quá dài) thì không ghi nhận, quay về trang chi tiết tour kèm thông báo “Vui lòng nhập đầy đủ và đúng thông tin đánh giá.”
5.1|S|Nếu hệ thống phát hiện tài khoản đã đánh giá tour này rồi thì từ chối ghi nhận, thông báo “Bạn đã đánh giá tour này rồi.”
5.2|S|Nếu hệ thống phát hiện tài khoản chưa có đơn hoàn thành chứa tour này thì từ chối ghi nhận, thông báo “Bạn chỉ có thể đánh giá tour đã đặt và đơn đã hoàn tất.”
DIAGRAMS:
AC-UC010. Biểu đồ hoạt động - Đánh giá tour đã đi
SQ-UC010. Biểu đồ tuần tự - Đánh giá tour đã đi
>>END

### 2.7.11 UC011. Cập nhật hồ sơ cá nhân

>>USECASE
ID: UC011
NAME: Cập nhật hồ sơ cá nhân
PURPOSE: Cho phép khách hàng giữ thông tin liên hệ của mình luôn mới, để lần đặt tour sau không phải nhập lại từ đầu.
DESC: Khách hàng sửa họ tên, email, số điện thoại và địa chỉ trên trang hồ sơ cá nhân. Email mới phải chưa thuộc về tài khoản khác; từ lần đăng nhập sau, khách dùng email mới để đăng nhập.
ACTORS: Khách hàng
PRE: Khách hàng đã đăng nhập [«include» UC005].
POST: Thông tin hồ sơ được cập nhật; các đơn đã đặt trước đó không bị ảnh hưởng vì thông tin liên hệ đã được chép riêng vào từng đơn.
MAIN:
1|A|Mở menu tài khoản, chọn “Thông tin cá nhân”
2|S|Hiển thị biểu mẫu với dữ liệu hiện có của tài khoản
3|A|Sửa họ tên, email, số điện thoại và/hoặc địa chỉ, bấm “Lưu”
4|S|Kiểm tra ràng buộc dữ liệu nhập: họ tên, email, số điện thoại bắt buộc; email đúng định dạng; số điện thoại đúng định dạng Việt Nam
5|S|Kiểm tra email chưa thuộc về tài khoản khác (email của chính khách thì hợp lệ)
6|S|Cập nhật tài khoản, chuyển hướng lại trang hồ sơ kèm thông báo cập nhật thành công
ALT:
4.1|S|Nếu dữ liệu nhập thiếu hoặc sai định dạng thì trả lại biểu mẫu kèm thông báo lỗi tại đúng ô nhập, không cập nhật
5.1|S|Nếu email mới đã thuộc về tài khoản khác thì trả lại biểu mẫu kèm thông báo lỗi “Email … đã được sử dụng.” tại ô email, không cập nhật
DIAGRAMS:
AC-UC011. Biểu đồ hoạt động - Cập nhật hồ sơ cá nhân
SQ-UC011. Biểu đồ tuần tự - Cập nhật hồ sơ cá nhân
>>END

### 2.7.12 UC012. Đổi mật khẩu

>>USECASE
ID: UC012
NAME: Đổi mật khẩu
PURPOSE: Cho phép khách hàng tự đổi mật khẩu định kỳ để bảo vệ tài khoản.
DESC: Khách hàng nhập mật khẩu hiện tại để xác thực, sau đó nhập mật khẩu mới và xác nhận; hệ thống kiểm tra mật khẩu mới trước, rồi mới so khớp mật khẩu hiện tại và mã hoá mật khẩu mới trước khi lưu.
ACTORS: Khách hàng
PRE: Khách hàng đã đăng nhập [«include» UC005].
POST: Mật khẩu của tài khoản được thay bằng mật khẩu mới; các phiên đăng nhập khác không bị ảnh hưởng ngay lập tức.
MAIN:
1|A|Mở menu tài khoản, chọn “Đổi mật khẩu”
2|S|Hiển thị biểu mẫu gồm ba ô: mật khẩu hiện tại, mật khẩu mới, nhập lại mật khẩu mới
3|A|Nhập đủ ba ô, bấm “Đổi mật khẩu”
4|S|Kiểm tra dữ liệu nhập: cả ba ô bắt buộc, mật khẩu mới từ 6 đến 50 ký tự và khớp với ô nhập lại
5|S|Kiểm tra mật khẩu hiện tại có khớp với mật khẩu đã lưu của tài khoản không
6|S|Mã hoá mật khẩu mới, cập nhật cho tài khoản và hiển thị lại trang đổi mật khẩu kèm thông báo thành công
ALT:
4.1|S|Nếu mật khẩu mới không đạt độ dài, hoặc không khớp với ô nhập lại, hoặc có ô bị bỏ trống thì trả lại biểu mẫu kèm thông báo lỗi tại đúng ô, không đổi mật khẩu (trường hợp không khớp: lỗi hiện ở ô nhập lại mật khẩu mới)
5.1|S|Nếu mật khẩu hiện tại nhập sai thì trả lại biểu mẫu kèm thông báo “Mật khẩu hiện tại không đúng.” tại ô mật khẩu hiện tại, không đổi mật khẩu
DIAGRAMS:
AC-UC012. Biểu đồ hoạt động - Đổi mật khẩu
SQ-UC012. Biểu đồ tuần tự - Đổi mật khẩu
>>END

### 2.7.13 UC013. Quản lý danh mục tour

>>USECASE
ID: UC013
NAME: Quản lý danh mục tour
PURPOSE: Cho quản trị viên tổ chức các tour theo nhóm danh mục để phục vụ việc tra cứu của khách: tạo danh mục mới, cập nhật danh mục đã có và loại bỏ danh mục không còn dùng, đồng thời bảo vệ dữ liệu khỏi bị xoá nhầm khi danh mục vẫn còn tour. Đây là use case đại diện cho mẫu thao tác Thêm - Sửa - Xoá dùng chung cho các chức năng quản lý khác của quản trị viên.
DESC: Quản trị viên vào trang “Quản lý danh mục” (có ô tìm theo từ khoá, xem chi tiết danh mục kèm các tour thuộc danh mục, và bật/tắt hiển thị). Từ đó thực hiện luồng chính Thêm danh mục, hoặc các luồng mở rộng («extend») Sửa danh mục và Xoá danh mục. Khi thêm hoặc sửa, hệ thống kiểm tra tên chưa trùng danh mục nào khác và tự sinh đường dẫn thân thiện duy nhất từ tên. Khi xoá, hệ thống chỉ cho xoá danh mục không còn tour nào; ngược lại từ chối kèm gợi ý phương án thay thế.
ACTORS: Quản trị viên
PRE: Quản trị viên đã đăng nhập [«include» UC005]; với luồng Sửa và Xoá thì danh mục cần thao tác đã tồn tại.
POST: Thêm: một danh mục mới được tạo, hiển thị ngay ở danh sách công khai nếu đang bật hiển thị, đường dẫn thân thiện được sinh tự động và không trùng danh mục nào khác. Sửa: thông tin danh mục được cập nhật; nếu đổi tên thì đường dẫn thân thiện đổi theo và vẫn không trùng. Xoá: danh mục không còn tour thì bị xoá khỏi hệ thống; danh mục còn tour thì giữ nguyên, không có gì thay đổi.
MAIN:
1|A|Mở khu quản trị, chọn “Quản lý danh mục”, bấm “Thêm danh mục”
2|S|Hiển thị biểu mẫu thêm danh mục trống
3|A|Nhập tên danh mục (bắt buộc), mô tả, chọn ảnh đại diện (tuỳ chọn), tick/bỏ tick “Hiển thị”, bấm “Lưu”
4|S|Kiểm tra dữ liệu nhập (tên tối đa 100 ký tự, mô tả tối đa 500 ký tự) rồi kiểm tra tên danh mục chưa tồn tại
5|S|Sinh đường dẫn thân thiện duy nhất từ tên danh mục, lưu ảnh đại diện nếu có, tạo danh mục mới
6|S|Hiển thị lại danh sách danh mục kèm thông báo tạo thành công
ALT:
3.1|S|Nếu để trống tên, hoặc tên/mô tả vượt quá độ dài cho phép thì trả lại biểu mẫu kèm thông báo lỗi tại đúng ô tương ứng
4.1|S|Nếu tên danh mục đã tồn tại thì trả lại biểu mẫu kèm thông báo lỗi tại ô tên, giữ nguyên các giá trị đã nhập
4.2|S|Nếu tệp ảnh đại diện không hợp lệ (sai định dạng) thì không lưu gì, trả lại biểu mẫu kèm thông báo lỗi
S1|A|[«extend» Sửa danh mục] Từ danh sách danh mục, bấm “Sửa” ở một danh mục
S2|S|Hiển thị biểu mẫu sửa với dữ liệu hiện tại của danh mục đó
S3|A|Sửa tên, mô tả, ảnh đại diện hoặc trạng thái hiển thị, bấm “Lưu”
S4|S|Kiểm tra dữ liệu nhập và kiểm tra tên (sau khi sửa) chưa trùng với danh mục khác (trừ chính nó)
S5|S|Sinh lại đường dẫn thân thiện từ tên (giữ không trùng); nếu có ảnh mới thì lưu ảnh mới và cập nhật danh mục; sau khi lưu thành công, ảnh cũ bị xoá nếu là ảnh do quản trị viên tự tải lên (không xoá ảnh có sẵn của hệ thống)
S6|S|Hiển thị lại danh sách kèm thông báo cập nhật thành công
S4.1|S|Nếu để trống tên, tên/mô tả quá dài, hoặc tên mới trùng một danh mục khác thì trả lại biểu mẫu kèm thông báo lỗi tại ô tên/ô tương ứng, giữ nguyên các giá trị vừa sửa
X1|A|[«extend» Xoá danh mục] Từ danh sách danh mục, bấm “Xoá” ở một danh mục
X2|S|Hiển thị hộp xác nhận “Bạn chắc chắn muốn xoá mục này?”
X3|A|Xác nhận xoá
X4|S|Đếm số tour đang thuộc danh mục đó
X5|S|Nếu bằng 0 thì xoá danh mục (và ảnh đại diện nếu là ảnh tự tải lên)
X6|S|Báo xoá thành công ngay trên trang danh sách và bỏ dòng danh mục vừa xoá, không tải lại trang
X3.1|S|Nếu quản trị viên không đồng ý ở hộp xác nhận thì không xoá, danh mục giữ nguyên
X5.1|S|Nếu danh mục còn tour thì không xoá gì cả, hiện thông báo “Không xoá được: danh mục này còn N tour. Hãy chuyển các tour đó sang danh mục khác trước, hoặc dùng “Ẩn danh mục”.” ngay trên trang danh sách
DIAGRAMS:
AC-UC013-Them. Biểu đồ hoạt động - Thêm danh mục tour
SQ-UC013-Them. Biểu đồ tuần tự - Thêm danh mục tour
AC-UC013-Sua. Biểu đồ hoạt động - Sửa danh mục tour
SQ-UC013-Sua. Biểu đồ tuần tự - Sửa danh mục tour
AC-UC013-Xoa. Biểu đồ hoạt động - Xoá danh mục tour
SQ-UC013-Xoa. Biểu đồ tuần tự - Xoá danh mục tour
>>END

### 2.7.14 UC016. Quản lý đơn đặt tour

>>USECASE
ID: UC016
NAME: Quản lý đơn đặt tour
PURPOSE: Cho phép quản trị viên theo dõi và xử lý toàn bộ vòng đời của các đơn đặt tour do khách hàng gửi lên.
DESC: Quản trị viên xem danh sách đơn (lọc theo trạng thái, tìm theo mã đơn/tên/email khách), mở chi tiết một đơn, đổi trạng thái theo đúng vòng đời hợp lệ (kể cả huỷ đơn), sửa số lượng khách của một dòng và đánh dấu đã thu tiền. Đơn chỉ được sinh ra khi khách đặt tour (UC006) và không bao giờ bị xoá để giữ dấu vết đối soát; thay cho việc xoá, đơn được chuyển sang “Đã huỷ”. Vì vậy chức năng này không có thao tác Thêm hay Xoá đơn, chỉ có thao tác Sửa (đổi trạng thái, sửa số khách, ghi nhận thanh toán). Mỗi lần đổi trạng thái, hệ thống ghi lại một mốc mới trong dòng lịch sử của đơn (không sửa các mốc cũ), kèm người thực hiện và lý do nếu quản trị viên có nhập.
ACTORS: Quản trị viên
PRE: Quản trị viên đã đăng nhập [«include» UC005].
POST: Trạng thái đơn hoặc số lượng khách được cập nhật đúng yêu cầu; đơn chỉ đi theo đúng bốn hướng chuyển hợp lệ (Chờ xác nhận → Đã xác nhận → Hoàn thành, hoặc Chờ xác nhận/Đã xác nhận → Đã huỷ), “Hoàn thành” và “Đã huỷ” là hai trạng thái cuối, không chuyển tiếp hay sửa được nữa; chuyển sang “Hoàn thành” chỉ được chấp nhận khi đã tới ngày khởi hành và đơn đã có khoản thanh toán được xác nhận; nếu huỷ đơn, số chỗ đã giữ được hoàn lại, lượt dùng mã giảm giá (nếu có) được trả lại, các khoản thanh toán đang chờ được đóng và nếu đơn đã có khoản thanh toán được xác nhận thì phát sinh thêm một khoản hoàn tiền bằng 100% số tiền đã thu; mọi lần đổi trạng thái đều được ghi thêm một mốc mới vào dòng lịch sử của đơn kèm người đổi và lý do; khi đánh dấu đã thu tiền, khoản thanh toán tương ứng chuyển sang “Đã thanh toán” kèm thời điểm và mã giao dịch; khi sửa số lượng khách, danh sách hành khách của dòng đó luôn được giữ khớp đúng số người lớn/trẻ em mới.
MAIN:
1|A|Mở khu quản trị, chọn “Đơn đặt tour”
2|S|Hiển thị danh sách đơn có phân trang, có thể lọc theo trạng thái và tìm theo từ khoá, sắp theo ngày đặt gần nhất
3|A|Bấm vào một đơn để xem chi tiết
4|S|Hiển thị đầy đủ thông tin đơn: khách, các dòng chi tiết, hành khách, tổng tiền, mã giảm giá (nếu có), dòng thời gian đổi trạng thái, danh sách các lần thanh toán và các trạng thái có thể chuyển tới
5|A|Chọn trạng thái mới cho đơn (ví dụ từ “Chờ xác nhận” sang “Đã xác nhận”), nhập lý do (tuỳ chọn), bấm “Cập nhật”
6|S|Kiểm tra trạng thái mới có nằm trong các hướng chuyển hợp lệ hay không, và riêng với “Hoàn thành” thì đơn phải đã tới ngày khởi hành và đã có khoản thanh toán được xác nhận; nếu hợp lệ, ghi trạng thái mới cho đơn và thêm một mốc mới vào dòng lịch sử (trạng thái cũ, trạng thái mới, quản trị viên đang thao tác, lý do)
7|S|Nếu trạng thái mới là “Đã huỷ” thì hoàn lại số chỗ đã giữ cho từng lịch khởi hành trong đơn, trả lại lượt dùng mã giảm giá nếu có, đóng các khoản thanh toán đang chờ, và nếu đơn đã có khoản thanh toán được xác nhận thì ghi thêm một khoản hoàn tiền bằng 100% số tiền đã thu (huỷ do phía công ty nên hoàn toàn bộ, khác mức hoàn theo tỉ lệ khi chính khách tự huỷ ở UC008)
8|S|Hiển thị lại chi tiết đơn kèm thông báo cập nhật trạng thái thành công
ALT:
6.1|S|Nếu trạng thái mới không nằm trong các hướng chuyển hợp lệ thì từ chối kèm thông báo “Không thể chuyển đơn từ trạng thái … sang …”, giữ nguyên đơn
6.2|S|Nếu trạng thái mới là “Hoàn thành” nhưng chưa tới ngày khởi hành, hoặc đơn chưa có khoản nào được xác nhận đã thu tiền, thì từ chối chuyển trạng thái kèm thông báo lý do tương ứng
S1|A|[«extend» Sửa số khách] Nhập số người lớn/trẻ em mới cho một dòng chi tiết và bấm lưu
S2|S|Từ chối nếu đơn đã ở trạng thái cuối hoặc đã có khoản thanh toán được xác nhận đã thu tiền, kèm thông báo lý do; giữ nguyên số liệu
S3|S|Nếu tăng số lượng một loại khách mà chưa nhập đủ tên hành khách mới tương ứng thì chưa áp dụng bất kỳ thay đổi nào (kể cả số chỗ và thành tiền), chỉ hiển thị thêm đúng số ô còn thiếu để quản trị viên nhập tiếp
S4|S|Nếu số khách mới vượt quá số chỗ còn trống của lịch khởi hành, hoặc không hợp lệ (ít nhất 1 người lớn, tối đa 30 khách mỗi dòng) thì từ chối, giữ nguyên số liệu cũ, hiển thị thông báo lỗi
S5|S|Nếu hợp lệ: tính lại thành tiền của dòng, tổng tiền của đơn, giữ thêm hoặc trả bớt chỗ của lịch khởi hành tương ứng, tính lại số tiền giảm giá nếu đơn đang áp mã khuyến mãi, cập nhật số tiền của khoản thanh toán đang chờ, đồng bộ danh sách hành khách của dòng cho khớp số người lớn/trẻ em mới (khi giảm thì loại bỏ những hành khách thêm gần đây nhất của đúng loại đó và nêu rõ tên người bị loại trong thông báo kết quả)
T1|A|[«extend» Sửa - ghi nhận thanh toán] Bấm “Đánh dấu đã thu tiền” ở một khoản thanh toán đang “Chờ thanh toán”, nhập mã giao dịch (tuỳ chọn)
T2|S|Kiểm tra đơn chưa bị huỷ, khoản thanh toán còn đang chờ và mã giao dịch chưa được dùng cho một lần thanh toán khác; nếu hợp lệ thì chuyển khoản thanh toán sang “Đã thanh toán”, ghi lại thời điểm và mã giao dịch
T3|S|Nếu không hợp lệ (đơn đã huỷ, khoản thanh toán không còn đang chờ, hoặc mã giao dịch đã được dùng) thì từ chối, giữ nguyên khoản thanh toán cũ, hiển thị thông báo lỗi
DIAGRAMS:
AC-UC016. Biểu đồ hoạt động - Quản lý đơn đặt tour
SQ-UC016-P1. Biểu đồ tuần tự - Quản lý đơn đặt tour (phần 1/2)
SQ-UC016-P2. Biểu đồ tuần tự - Quản lý đơn đặt tour (phần 2/2)
>>END

### 2.7.15 UC019. Xem thống kê doanh thu

>>USECASE
ID: UC019
NAME: Xem thống kê doanh thu
PURPOSE: Giúp quản trị viên nắm nhanh tình hình kinh doanh mà không cần cộng sổ sách thủ công.
DESC: Trang tổng quan khu quản trị hiển thị các số liệu khái quát (số tour, số khách hàng đang hoạt động, số đơn, tổng doanh thu, số đơn theo từng trạng thái), danh sách tour bán chạy, 5 đơn mới nhất và một biểu đồ doanh thu theo tháng; biểu đồ được tải và vẽ ngay khi trang mở xong, có nút tải lại.
ACTORS: Quản trị viên
PRE: Quản trị viên đã đăng nhập [«include» UC005].
POST: Trang tổng quan hiển thị đúng số liệu tổng hợp tính tới thời điểm truy cập.
MAIN:
1|A|Đăng nhập với vai trò quản trị viên (hoặc chọn “Quản trị”), hệ thống đưa tới trang tổng quan khu quản trị
2|S|Tính các số liệu khái quát: tổng số tour (kể cả tour ngừng bán), số khách hàng đang hoạt động, tổng số đơn, tổng doanh thu, số đơn theo từng trạng thái; lấy danh sách 5 tour bán chạy nhất và 5 đơn mới nhất
3|S|Hiển thị các số liệu khái quát dưới dạng thẻ số liệu cùng danh sách tour bán chạy và đơn mới nhất
4|S|Khi trang mở xong, truy xuất số liệu doanh thu theo tháng
5|S|Tổng hợp doanh thu theo từng tháng đặt trong 12 tháng gần nhất, chỉ tính các đơn đang ở trạng thái “Đã xác nhận” hoặc “Hoàn thành”; đơn “Chờ xác nhận” (chưa chắc thành công) và đơn “Đã huỷ” không được cộng vào doanh thu
6|S|Hiển thị biểu đồ cột doanh thu theo tháng, có nhãn số tiền và bảng số liệu chi tiết
ALT:
5.1|S|Nếu chưa có đơn nào ở trạng thái “Đã xác nhận”/“Hoàn thành” trong 12 tháng gần nhất thì biểu đồ hiển thị trạng thái “chưa có dữ liệu”
6.1|A|Bấm nút tải lại biểu đồ
6.2|S|Truy xuất và vẽ lại biểu đồ với số liệu mới nhất, không tải lại trang
DIAGRAMS:
AC-UC019. Biểu đồ hoạt động - Xem thống kê doanh thu
SQ-UC019. Biểu đồ tuần tự - Xem thống kê doanh thu
>>END

### 2.7.16 UC020. Kiểm duyệt đánh giá

>>USECASE
ID: UC020
NAME: Kiểm duyệt đánh giá
PURPOSE: Ngăn nội dung không phù hợp hoặc chưa xác thực hiển thị công khai ngay lập tức; cho quản trị viên quyền quyết định cuối cùng trước khi một đánh giá được khách hàng khác nhìn thấy.
DESC: Đây là màn hình quản trị tiếp nối sau khi khách hàng gửi đánh giá (UC010): mọi đánh giá gửi lên đều ở trạng thái chờ duyệt cho tới khi quản trị viên xử lý ở đây. Quản trị viên có thể duyệt, bỏ duyệt, trả lời hoặc xoá một đánh giá.
ACTORS: Quản trị viên
PRE: Quản trị viên đã đăng nhập [«include» UC005]; có ít nhất một đánh giá đang chờ duyệt (kết quả của UC010) để thao tác.
POST: Đánh giá được duyệt thì hiện ra trang công khai và được tính vào điểm trung bình của tour; đánh giá bị bỏ duyệt thì biến mất khỏi trang công khai và không còn tính vào điểm trung bình; đánh giá có thể được trả lời hoặc xoá hẳn.
MAIN:
1|A|Mở khu quản trị, chọn “Kiểm duyệt đánh giá” (huy hiệu trên menu hiện số đánh giá đang chờ duyệt)
2|S|Hiển thị danh sách phân trang, mặc định lọc theo trạng thái “Chờ duyệt”; mỗi dòng gồm tour, người viết, số sao, trích đoạn nội dung, ngày gửi, và nhãn “Đã xác thực” nếu đánh giá gắn với một đơn đã hoàn thành thật
3|A|Chuyển bộ lọc trạng thái sang “Đã duyệt” hoặc “Tất cả” để xem lại các đánh giá khác
4|A|Bấm “Duyệt” ở một đánh giá đang chờ
5|S|Đánh dấu đánh giá là đã duyệt; đánh giá lập tức hiện ở trang chi tiết tour tương ứng và được tính vào điểm trung bình; hiển thị lại danh sách kèm thông báo “Đã duyệt đánh giá.”, huy hiệu số lượng chờ duyệt giảm đi một
6|A|Bấm “Trả lời” ở một đánh giá, nhập nội dung phản hồi, bấm gửi
7|S|Ghi lại câu trả lời của quản trị viên; câu trả lời hiện ngay dưới đánh giá đó ở trang công khai (nếu đánh giá đã được duyệt)
ALT:
4.1|A|Bấm “Bỏ duyệt” ở một đánh giá đã duyệt trước đó
4.2|S|Đánh dấu đánh giá là chưa duyệt; đánh giá biến mất khỏi trang công khai ngay lập tức và không còn được tính vào điểm trung bình của tour, nhưng vẫn còn trong danh sách quản trị
4.3|A|Bấm “Xoá” ở một đánh giá bất kỳ
4.4|S|Xoá hẳn đánh giá; nếu đánh giá đó đang hiện công khai thì biến mất ngay, điểm trung bình của tour được tính lại không còn đánh giá đó
6.1|S|Nếu nội dung trả lời để trống thì từ chối kèm thông báo “Vui lòng nhập nội dung trả lời.”; có thể trả lời cả đánh giá chưa duyệt (câu trả lời chỉ hiện công khai khi đánh giá đã được duyệt)
DIAGRAMS:
AC-UC020. Biểu đồ hoạt động - Kiểm duyệt đánh giá
SQ-UC020. Biểu đồ tuần tự - Kiểm duyệt đánh giá
>>END

### 2.7.17 UC021. Duyệt liên hệ

>>USECASE
ID: UC021
NAME: Duyệt liên hệ
PURPOSE: Cho quản trị viên theo dõi và xử lý các liên hệ khách gửi từ UC002, không để liên hệ nào bị bỏ sót.
DESC: Danh sách liên hệ được sắp theo thứ tự mới nhất trước, mặc định mở ở nhóm “Mới”. Quản trị viên xem nội dung và xử lý ngay tại chỗ, không cần mở trang riêng - cùng cách trình bày với chức năng kiểm duyệt đánh giá. Khi đổi sang trạng thái “Đã xử lý” hoặc “Thư rác”, hệ thống tự động ghi lại người xử lý và thời điểm xử lý. Menu quản trị luôn hiện số lượng liên hệ mới chưa xử lý. Mục menu của chức năng này có tên “Quản lý liên hệ”.
ACTORS: Quản trị viên
PRE: Quản trị viên đã đăng nhập [«include» UC005]; có ít nhất một liên hệ do UC002 gửi lên.
POST: Trạng thái liên hệ được cập nhật đúng yêu cầu; khi chuyển “Đã xử lý”/“Thư rác” thì người xử lý và thời điểm xử lý được ghi lại; số lượng liên hệ mới trên menu giảm tương ứng.
MAIN:
1|A|Mở khu quản trị, chọn “Quản lý liên hệ”
2|S|Mặc định lọc “Mới”, hiển thị danh sách phân trang kèm người gửi, tour đang hỏi (nếu có), tiêu đề, ngày gửi
3|A|Chuyển bộ lọc sang “Đang xử lý”/“Đã xử lý”/“Thư rác”/“Tất cả” để xem các liên hệ khác
4|A|Mở một liên hệ, đọc nội dung, nhập ghi chú trả lời (tuỳ chọn), chọn trạng thái mới (ví dụ “Đã xử lý”), bấm cập nhật
5|S|Ghi nhận trạng thái mới và ghi chú trả lời; nếu chuyển sang “Đã xử lý” hoặc “Thư rác” thì ghi thêm người xử lý là quản trị viên đang đăng nhập và thời điểm xử lý
6|S|Hiển thị lại danh sách kèm thông báo “Đã cập nhật liên hệ.”; số lượng liên hệ mới trên menu cập nhật lại
ALT:
5.1|S|Nếu quản trị viên chỉ đổi sang “Đang xử lý” thì chưa ghi người xử lý và thời điểm xử lý, liên hệ vẫn còn trong hàng đợi
5.2|S|Nếu đổi ngược từ “Đã xử lý”/“Thư rác” về trạng thái khác thì người xử lý và thời điểm xử lý đã ghi trước đó được giữ nguyên
DIAGRAMS:
AC-UC021. Biểu đồ hoạt động - Duyệt liên hệ
SQ-UC021. Biểu đồ tuần tự - Duyệt liên hệ
>>END

## 2.8 Biểu đồ lớp

[[HINH: 02. Biểu đồ lớp - Mô hình miền (lõi)]]

[[HINH: 02b. Biểu đồ lớp - Chín bảng bổ sung]]

Mô hình miền được chia làm hai lớp biểu đồ để dễ đọc: biểu đồ lõi gồm bảy thực thể trung tâm (`TourCategory`, `Tour`, `TourDeparture`, `User`, `Booking`, `BookingDetail`, cùng các quan hệ chính), và biểu đồ chín bảng bổ sung (`TourImage`, `TourItinerary`, `BookingPassenger`, `BookingStatusHistory`, `Payment`, `Promotion`, `CouponUsage`, `Review`, `ContactMessage`, `PasswordResetToken`) mở rộng mô hình để đáp ứng đầy đủ các yêu cầu nâng cao. Chi tiết từng thực thể được đặc tả trong mục 3.2.

# CHƯƠNG 3: THIẾT KẾ VÀ HIỆN THỰC

## 3.1 Mô hình lớp thực thể (Class Diagram)

[[HINH: 02. Biểu đồ lớp - Mô hình miền (lõi)]]

[[HINH: 02b. Biểu đồ lớp - Chín bảng bổ sung]]

Toàn bộ mô hình gồm 16 thực thể ứng với 17 bảng trong cơ sở dữ liệu, cộng hai lớp `Cart`/`CartItem` dùng riêng cho giỏ hàng trong phiên làm việc, không có bảng tương ứng. Quan hệ trung tâm của mô hình là chuỗi Danh mục tour → Tour → Đợt khởi hành → Dòng chi tiết đơn → Đơn đặt tour → Tài khoản, phản ánh đúng lý do nghiệp vụ: một chương trình tour có thể mở nhiều đợt khởi hành với giá và số chỗ khác nhau, và một đơn có thể gồm nhiều dòng đặt cho các đợt khởi hành khác nhau.

## 3.2 Đặc tả sơ đồ lớp

### 3.2.1 Thực thể TourCategory

>>ENTITY
NAME: TourCategory
TABLE: tour_categories
DESC: Danh mục tour, ví dụ "Du lịch biển", "Du lịch nước ngoài".
FIELD: id|Long|Khoá chính, tự tăng|
FIELD: name|String|Bắt buộc, duy nhất, tối đa 100 ký tự|
FIELD: slug|String|Bắt buộc, duy nhất, tối đa 120 ký tự|Dạng thân thiện URL, sinh tự động từ tên
FIELD: description|String|Tối đa 500 ký tự|
FIELD: imageUrl|String|Tối đa 255 ký tự|
FIELD: active|boolean|Bắt buộc, mặc định true|true: đang hiển thị công khai
FIELD: createdAt / updatedAt|LocalDateTime|Tự động ghi nhận, không cần nhập tay|Ngày tạo / ngày cập nhật gần nhất
METHOD: equals(Object)/hashCode()|boolean/int|So sánh theo tên đường dẫn thân thiện
METHOD: Getter/Setter|-|Theo ràng buộc trên
>>END

### 3.2.2 Thực thể Tour

>>ENTITY
NAME: Tour
TABLE: tours
DESC: Một chương trình tour được rao bán, ví dụ "Đà Nẵng - Hội An 4N3Đ". Chỉ mô tả chương trình; ngày đi cụ thể, giá và số chỗ còn lại nằm ở đợt khởi hành.
FIELD: id|Long|Khoá chính, tự tăng|
FIELD: code|String|Bắt buộc, duy nhất, tối đa 30 ký tự|VD: DN-HA-4N3D
FIELD: name|String|Bắt buộc, tối đa 200 ký tự|
FIELD: slug|String|Bắt buộc, duy nhất, tối đa 250 ký tự|
FIELD: shortDescription|String|Tối đa 500 ký tự|Hiện trên thẻ tour ở trang danh sách
FIELD: description|String|Văn bản dài|
FIELD: itinerary|String|Văn bản dài|Lịch trình dạng văn bản (dữ liệu cũ)
FIELD: departureLocation|String|Bắt buộc, tối đa 100 ký tự|
FIELD: destination|String|Bắt buộc, tối đa 100 ký tự|
FIELD: durationDays / durationNights|Integer|Bắt buộc|Số ngày / số đêm
FIELD: basePrice|BigDecimal|Bắt buộc|Giá "từ..." dùng để hiển thị và lọc theo khoảng giá
FIELD: thumbnail|String|Tối đa 255 ký tự|Đường dẫn tương đối ảnh đại diện
FIELD: transportation|String|Tối đa 100 ký tự|
FIELD: featured|boolean|Bắt buộc, mặc định false|Tour nổi bật đưa lên trang chủ
FIELD: active|boolean|Bắt buộc, mặc định true|Ngừng bán nhưng vẫn giữ dữ liệu
FIELD: viewCount|Long|Bắt buộc, mặc định 0|
FIELD: searchText|String|Tối đa 500 ký tự|Bản sao đã bỏ dấu và hạ chữ thường, tự động tạo lại mỗi khi lưu, phục vụ tìm không dấu
REL: category|TourCategory|N-1, bắt buộc|Danh mục của tour
REL: images|Danh sách TourImage|1-N|Ảnh thuộc sở hữu của tour
REL: itineraries|Danh sách TourItinerary|1-N|Lịch trình có cấu trúc theo từng ngày
REL: departures|Danh sách TourDeparture|1-N|Các đợt khởi hành
METHOD: buildSearchText()|void|Tự động tạo lại nội dung tìm kiếm không dấu mỗi khi lưu tour
METHOD: getDurationText()|String|Trả về chuỗi "X ngày Y đêm"
METHOD: getArtVariant()|int|Chọn 1 trong 6 khung cảnh minh hoạ khi tour chưa có ảnh, dựa theo tên điểm đến
METHOD: addImage(TourImage)/addDeparture(TourDeparture)|void|Giữ hai đầu quan hệ nhất quán
METHOD: Getter/Setter|-|Theo ràng buộc trên
>>END

### 3.2.3 Thực thể TourImage

>>ENTITY
NAME: TourImage
TABLE: tour_images
DESC: Một ảnh trong bộ sưu tập ảnh của tour. Lưu đường dẫn tương đối, không lưu đường dẫn tuyệt đối của máy chủ.
FIELD: id|Long|Khoá chính, tự tăng|
FIELD: imagePath|String|Bắt buộc, tối đa 255 ký tự|VD: tours/anh-minh-hoa.jpg
FIELD: caption|String|Tối đa 255 ký tự|
FIELD: sortOrder|Integer|Bắt buộc, mặc định 0|Thứ tự hiển thị, số nhỏ đứng trước
REL: tour|Tour|N-1, bắt buộc|Tour sở hữu ảnh này
METHOD: equals(Object)/hashCode()|boolean/int|So sánh theo đường dẫn ảnh
METHOD: Getter/Setter|-|Theo ràng buộc trên
>>END

### 3.2.4 Thực thể TourItinerary

>>ENTITY
NAME: TourItinerary
TABLE: tour_itineraries
DESC: Một ngày trong lịch trình của tour, ví dụ "Ngày 2 - Vịnh Hạ Long". Ràng buộc duy nhất: một tour không có hai "ngày 2".
FIELD: id|Long|Khoá chính, tự tăng|
FIELD: dayNo|Integer|Bắt buộc|Ngày thứ mấy của hành trình, bắt đầu từ 1
FIELD: title|String|Bắt buộc, tối đa 200 ký tự|
FIELD: description|String|Tối đa 2000 ký tự|
FIELD: meals|String|Tối đa 100 ký tự|VD: "Sáng, Trưa, Tối"
FIELD: accommodation|String|Tối đa 150 ký tự|Nơi nghỉ đêm
REL: tour|Tour|N-1, bắt buộc|Tour sở hữu ngày lịch trình này
METHOD: equals(Object)/hashCode()|boolean/int|So sánh theo mã định danh
METHOD: Getter/Setter|-|Theo ràng buộc trên
>>END

### 3.2.5 Thực thể TourDeparture

>>ENTITY
NAME: TourDeparture
TABLE: tour_departures
DESC: Một đợt khởi hành cụ thể của tour - ngày đi, giá, số chỗ. Đây là thứ khách hàng thực sự đặt. Ràng buộc duy nhất: một tour không có hai đợt khởi hành cùng ngày.
FIELD: id|Long|Khoá chính, tự tăng|
FIELD: departureDate|LocalDate|Bắt buộc|
FIELD: returnDate|LocalDate|Bắt buộc|Phải sau ngày khởi hành
FIELD: totalSeats|Integer|Bắt buộc|Tổng số chỗ của đợt
FIELD: availableSeats|Integer|Bắt buộc|Số chỗ còn trống; giảm khi đặt, tăng khi huỷ
FIELD: priceAdult / priceChild|BigDecimal|Bắt buộc|Giá người lớn / trẻ em
FIELD: active|boolean|Bắt buộc, mặc định true|
FIELD: version|Long|Bắt buộc|Trường kiểm soát phiên bản, dùng để tránh hai người cùng đặt trùng vào chỗ trống cuối cùng
REL: tour|Tour|N-1, bắt buộc|Tour cha của đợt khởi hành
METHOD: hasEnoughSeats(int)|boolean|Kiểm tra còn đủ chỗ cho số khách yêu cầu
METHOD: isBookable()|boolean|Còn nhận khách: đang mở bán, chưa tới ngày đi, còn chỗ
METHOD: holdSeats(int)|void|Giữ chỗ khi đặt tour, báo lỗi nếu không đủ chỗ
METHOD: releaseSeats(int)|void|Trả chỗ khi huỷ đơn hoặc giảm số khách
METHOD: getBookedSeats()|int|Số chỗ đã đặt, bằng tổng số chỗ trừ số chỗ còn trống
METHOD: Getter/Setter|-|Theo ràng buộc trên
>>END

### 3.2.6 Thực thể User

>>ENTITY
NAME: User
TABLE: users
DESC: Tài khoản người dùng - dùng chung cho khách hàng lẫn quản trị viên, phân biệt bằng vai trò. Không lưu ngược danh sách đơn của khách ngay trong tài khoản, để tránh nạp toàn bộ đơn của một khách vào bộ nhớ khi không cần.
FIELD: id|Long|Khoá chính, tự tăng|
FIELD: fullName|String|Bắt buộc, tối đa 100 ký tự|
FIELD: email|String|Bắt buộc, duy nhất, tối đa 150 ký tự|Dùng làm tên đăng nhập
FIELD: password|String|Bắt buộc, tối đa 100 ký tự|Được mã hoá, không lưu dạng chữ thường
FIELD: phone|String|Tối đa 20 ký tự|10-11 số, bắt đầu bằng số 0
FIELD: address|String|Tối đa 255 ký tự|
FIELD: role|Role|Bắt buộc, tối đa 20 ký tự|Khách hàng hoặc Quản trị viên
FIELD: enabled|boolean|Bắt buộc, mặc định true|Vô hiệu hoá tài khoản thay vì xoá khi còn dữ liệu tham chiếu
METHOD: isAdmin()|boolean|Kiểm tra tài khoản có vai trò Quản trị viên không
METHOD: equals(Object)/hashCode()|boolean/int|So sánh theo email (không phân biệt hoa/thường)
METHOD: Getter/Setter|-|Theo ràng buộc trên
>>END

### 3.2.7 Thực thể Booking

>>ENTITY
NAME: Booking
TABLE: bookings
DESC: Đơn đặt tour - gốc của cụm dữ liệu đơn và các dòng chi tiết. Chép lại thông tin liên hệ của khách tại thời điểm đặt (không đọc lại từ tài khoản) vì đơn hàng là chứng từ.
FIELD: id|Long|Khoá chính, tự tăng|
FIELD: code|String|Bắt buộc, duy nhất, tối đa 20 ký tự|VD: TB20260807A1B2C3
FIELD: bookingDate|LocalDateTime|Bắt buộc|
FIELD: customerName|String|Bắt buộc, tối đa 100 ký tự|Bản sao tại thời điểm đặt
FIELD: customerEmail|String|Bắt buộc, tối đa 150 ký tự|Bản sao tại thời điểm đặt
FIELD: customerPhone|String|Bắt buộc, tối đa 20 ký tự|Bản sao tại thời điểm đặt
FIELD: customerAddress|String|Tối đa 255 ký tự|
FIELD: totalAmount|BigDecimal|Bắt buộc|Đã trừ số tiền giảm giá, luôn được tính lại từ các dòng chi tiết
FIELD: discountAmount|BigDecimal|Bắt buộc|Chép lại tại thời điểm đặt
FIELD: status|BookingStatus|Bắt buộc, tối đa 20 ký tự|Chờ xác nhận / Đã xác nhận / Hoàn thành / Đã huỷ
FIELD: note|String|Tối đa 500 ký tự|
FIELD: paymentMethod|String|Tối đa 30 ký tự|
REL: user|User|N-1, bắt buộc|Chủ đơn
REL: promotion|Promotion|N-1, tuỳ chọn|Mã giảm giá đã áp, để trống nếu không dùng mã
REL: details|Danh sách BookingDetail|1-N|Các dòng chi tiết đơn
METHOD: getSubtotalAmount()|BigDecimal|Tổng tiền hàng trước khi trừ giảm giá
METHOD: recalculateTotal()|void|Tính lại tổng tiền, không cho số tiền giảm vượt quá tiền hàng
METHOD: getTotalGuests()|int|Tổng số khách của cả đơn
METHOD: isCancellable()|boolean|Trạng thái đang Chờ xác nhận hoặc Đã xác nhận
METHOD: addDetail(BookingDetail)/removeDetail(...)|void|Giữ hai đầu quan hệ nhất quán
METHOD: Getter/Setter|-|Theo ràng buộc trên
>>END

### 3.2.8 Thực thể BookingDetail

>>ENTITY
NAME: BookingDetail
TABLE: booking_details
DESC: Một dòng trong đơn đặt tour: đợt khởi hành nào, mấy người lớn, mấy trẻ em, thành tiền bao nhiêu. Tên tour và đơn giá được chép lại tại thời điểm đặt.
FIELD: id|Long|Khoá chính, tự tăng|
FIELD: tourNameSnapshot|String|Bắt buộc, tối đa 200 ký tự|Bản sao tên tour lúc đặt
FIELD: numAdults|Integer|Bắt buộc, mặc định 1|
FIELD: numChildren|Integer|Bắt buộc, mặc định 0|
FIELD: unitPriceAdult / unitPriceChild|BigDecimal|Bắt buộc|Đơn giá chép lại tại thời điểm đặt
FIELD: subtotal|BigDecimal|Bắt buộc|Thành tiền của dòng, luôn được tính lại khi đổi số khách
REL: booking|Booking|N-1, bắt buộc|Đơn cha
REL: departure|TourDeparture|N-1, bắt buộc|Đợt khởi hành đã đặt
REL: passengers|Danh sách BookingPassenger|1-N|Danh sách hành khách của dòng
METHOD: getTotalGuests()|int|Tổng số người lớn và trẻ em của dòng
METHOD: recalculateSubtotal()|void|Tính lại thành tiền theo số khách và đơn giá
METHOD: Getter/Setter|-|Theo ràng buộc trên
>>END

### 3.2.9 Thực thể BookingPassenger

>>ENTITY
NAME: BookingPassenger
TABLE: booking_passengers
DESC: Một hành khách cụ thể trong một dòng của đơn. Cho biết "ai đi" chứ không chỉ "bao nhiêu người". Quy tắc "số hành khách phải khớp số người lớn/trẻ em đã khai" được kiểm tra khi lưu.
FIELD: id|Long|Khoá chính, tự tăng|
FIELD: fullName|String|Bắt buộc, tối đa 100 ký tự|
FIELD: passengerType|PassengerType|Bắt buộc, tối đa 10 ký tự|Người lớn hoặc Trẻ em
FIELD: gender|Gender|Tối đa 10 ký tự|Nam / Nữ / Khác
FIELD: birthDate|LocalDate|Tuỳ chọn|Dùng đối chiếu giá trẻ em
FIELD: idNumber|String|Tối đa 30 ký tự|Số căn cước công dân hoặc hộ chiếu
FIELD: phone|String|Tối đa 20 ký tự|
FIELD: singleRoom|boolean|Bắt buộc, mặc định false|Yêu cầu phòng đơn
FIELD: note|String|Tối đa 255 ký tự|Yêu cầu riêng (ăn chay, xe lăn...)
REL: detail|BookingDetail|N-1, bắt buộc|Dòng chi tiết chứa hành khách này
METHOD: isAdult()|boolean|Kiểm tra hành khách có phải người lớn không
METHOD: Getter/Setter|-|Theo ràng buộc trên
>>END

### 3.2.10 Thực thể BookingStatusHistory

>>ENTITY
NAME: BookingStatusHistory
TABLE: booking_status_history
DESC: Nhật ký đổi trạng thái đơn - đơn nào, từ trạng thái nào sang trạng thái nào, ai đổi, lúc nào, vì lý do gì. Bảng chỉ thêm chứ không sửa. Ghi vào ngay từ khi khách đặt tour (mốc đầu tiên) và mỗi lần quản trị viên đổi trạng thái đơn - bảng này từng chỉ tồn tại để phục vụ quy tắc chặn xoá tài khoản, nay đã có dữ liệu thật được ứng dụng ghi vào, không đổi cấu trúc so với thiết kế ban đầu.
FIELD: id|Long|Khoá chính, tự tăng|
FIELD: fromStatus|BookingStatus|Tối đa 20 ký tự, có thể để trống|Để trống ở mốc đầu tiên
FIELD: toStatus|BookingStatus|Bắt buộc, tối đa 20 ký tự|
FIELD: reason|String|Tối đa 255 ký tự|
FIELD: changedAt|LocalDateTime|Bắt buộc, mặc định thời điểm tạo|
REL: booking|Booking|N-1, bắt buộc|Đơn liên quan
REL: changedBy|User|N-1, tuỳ chọn|Để trống nếu hệ thống tự đổi trạng thái
METHOD: Getter/Setter|-|Theo ràng buộc trên
>>END

### 3.2.11 Thực thể Payment

>>ENTITY
NAME: Payment
TABLE: payments
DESC: Một lần thanh toán của đơn hàng. Quan hệ một-nhiều với đơn vì có thể chia làm nhiều lần (đặt cọc, trả nốt). Mã giao dịch được kiểm tra trùng lặp trước khi ghi nhận. Một hồ sơ thanh toán ở trạng thái "Chờ thanh toán" được khởi tạo ngay lúc đặt tour; quản trị viên đánh dấu đã thu tiền thì chuyển sang "Đã thanh toán" - bảng này từng chỉ tồn tại để phục vụ quy tắc chặn xoá, nay đã có dữ liệu thật, không đổi cấu trúc so với thiết kế ban đầu.
FIELD: id|Long|Khoá chính, tự tăng|
FIELD: amount|BigDecimal|Bắt buộc, mặc định 0|
FIELD: method|PaymentMethod|Bắt buộc, tối đa 30 ký tự|Chuyển khoản ngân hàng / Tiền mặt / Ví điện tử
FIELD: status|PaymentStatus|Bắt buộc, tối đa 20 ký tự|Chờ thanh toán / Đã thanh toán / Thất bại / Đã hoàn tiền
FIELD: txnRef|String|Tối đa 50 ký tự, có thể để trống|Mã giao dịch ngân hàng/ví điện tử, không được trùng
FIELD: paidAt|LocalDateTime|Có thể để trống|Để trống khi chưa hoàn tất
FIELD: note|String|Tối đa 255 ký tự|
REL: booking|Booking|N-1, bắt buộc|Đơn được thanh toán
METHOD: markPaid(String txnRef)|void|Đổi trạng thái sang Đã thanh toán, ghi mã giao dịch và thời điểm
METHOD: isSettled()|boolean|Kiểm tra khoản thanh toán đã hoàn tất chưa
METHOD: Getter/Setter|-|Theo ràng buộc trên
>>END

### 3.2.12 Thực thể Promotion

>>ENTITY
NAME: Promotion
TABLE: promotions
DESC: Chương trình khuyến mãi / mã giảm giá. Toàn bộ điều kiện áp mã (còn hiệu lực, còn lượt, đơn đủ giá trị tối thiểu...) đều được kiểm tra khi sử dụng, không có ràng buộc nào đặt ở tầng cơ sở dữ liệu.
FIELD: id|Long|Khoá chính, tự tăng|
FIELD: code|String|Bắt buộc, duy nhất, tối đa 30 ký tự|VD: HELLO2026, luôn viết hoa
FIELD: name|String|Bắt buộc, tối đa 150 ký tự|
FIELD: description|String|Tối đa 500 ký tự|
FIELD: discountType|DiscountType|Bắt buộc, tối đa 20 ký tự|Theo phần trăm hoặc Theo số tiền cố định
FIELD: discountValue|BigDecimal|Bắt buộc, mặc định 0|Phần trăm (0-100) hoặc số tiền
FIELD: maxDiscount|BigDecimal|Có thể để trống|Trần giảm giá của loại theo phần trăm
FIELD: minOrderAmount|BigDecimal|Bắt buộc, mặc định 0|
FIELD: usageLimit|Integer|Có thể để trống|Để trống: không giới hạn tổng lượt
FIELD: usageLimitPerUser|Integer|Có thể để trống|Để trống: không giới hạn theo tài khoản
FIELD: usedCount|Integer|Bắt buộc, mặc định 0|Cộng dồn khi đặt tour thành công
FIELD: startAt / endAt|LocalDateTime|Bắt buộc|Khoảng thời gian hiệu lực
FIELD: active|boolean|Bắt buộc, mặc định true|
METHOD: isRunning(LocalDateTime)|boolean|Còn bật, trong thời hạn, còn lượt
METHOD: hasQuotaLeft()|boolean|Kiểm tra còn lượt sử dụng hay không
METHOD: calculateDiscount(BigDecimal)|BigDecimal|Tính số tiền giảm, có áp trần và không vượt quá giá trị đơn
METHOD: increaseUsedCount()|void|Cộng dồn số lượt đã dùng
METHOD: Getter/Setter|-|Theo ràng buộc trên
>>END

### 3.2.13 Thực thể CouponUsage

>>ENTITY
NAME: CouponUsage
TABLE: coupon_usages
DESC: Một lần mã giảm giá được dùng thật - mã nào, ai dùng, cho đơn nào, giảm bao nhiêu. Ràng buộc duy nhất: mỗi đơn chỉ áp một mã.
FIELD: id|Long|Khoá chính, tự tăng|
FIELD: discountAmount|BigDecimal|Bắt buộc, mặc định 0|Chép lại tại thời điểm đặt
FIELD: usedAt|LocalDateTime|Bắt buộc, mặc định thời điểm tạo|
REL: promotion|Promotion|N-1, bắt buộc|Mã đã dùng
REL: user|User|N-1, bắt buộc|Tài khoản đã dùng mã
REL: booking|Booking|N-1, bắt buộc, duy nhất|Đơn được áp mã
METHOD: Getter/Setter|-|Theo ràng buộc trên
>>END

### 3.2.14 Thực thể Review

>>ENTITY
NAME: Review
TABLE: reviews
DESC: Đánh giá của khách về một tour đã đi. Ràng buộc duy nhất: mỗi tài khoản đánh giá một tour một lần. Mặc định ở trạng thái chưa duyệt, phải qua kiểm duyệt mới hiển thị công khai. Đáng chú ý: các trường phục vụ kiểm duyệt (đã duyệt hay chưa, nội dung trả lời, thời điểm trả lời) đã có mặt trong thực thể này **ngay từ thiết kế ban đầu**, nên khi chức năng Kiểm duyệt đánh giá được bổ sung sau này, hệ thống chỉ việc đọc/ghi lại đúng các trường có sẵn đó mà không cần thay đổi cấu trúc dữ liệu. Đây là ví dụ cụ thể cho việc mô hình dữ liệu được thiết kế đón trước nhu cầu nghiệp vụ chứ không chắp vá khi có yêu cầu mới.
FIELD: id|Long|Khoá chính, tự tăng|
FIELD: rating|Integer|Bắt buộc, 1-5|
FIELD: title|String|Tối đa 200 ký tự|
FIELD: content|String|Bắt buộc, tối đa 1000 ký tự|
FIELD: approved|boolean|Bắt buộc, mặc định false|Chỉ đánh giá đã duyệt mới hiển thị công khai
FIELD: adminReply|String|Tối đa 1000 ký tự|Câu trả lời của quản trị viên
FIELD: repliedAt|LocalDateTime|Có thể để trống|
REL: tour|Tour|N-1, bắt buộc|Tour được đánh giá
REL: user|User|N-1, bắt buộc|Người đánh giá
REL: booking|Booking|N-1, tuỳ chọn|Đơn dùng làm bằng chứng đã đi tour
METHOD: isVerified()|boolean|Kiểm tra đánh giá có gắn với một đơn thật hay không
METHOD: reply(String)|void|Ghi câu trả lời của quản trị viên kèm mốc thời gian
METHOD: Getter/Setter|-|Theo ràng buộc trên
>>END

### 3.2.15 Thực thể ContactMessage

>>ENTITY
NAME: ContactMessage
TABLE: contact_messages
DESC: Một liên hệ gửi từ biểu mẫu công khai. Người gửi không bắt buộc có tài khoản nên lưu thẳng thông tin liên hệ thay vì trỏ sang tài khoản. Được ghi vào khi khách gửi liên hệ (trạng thái "Mới") và cập nhật khi quản trị viên xử lý (ghi nội dung trả lời, người xử lý, thời điểm xử lý khi chuyển "Đã xử lý"/"Thư rác") - bảng này từng chỉ tồn tại để phục vụ quy tắc chặn xoá tour, nay đã có chức năng thật ghi/đọc, không đổi cấu trúc so với thiết kế ban đầu.
FIELD: id|Long|Khoá chính, tự tăng|
FIELD: fullName|String|Bắt buộc, tối đa 100 ký tự|
FIELD: email|String|Bắt buộc, tối đa 150 ký tự|
FIELD: phone|String|Tối đa 20 ký tự|
FIELD: subject|String|Bắt buộc, tối đa 200 ký tự|
FIELD: content|String|Bắt buộc, tối đa 2000 ký tự|
FIELD: status|ContactStatus|Bắt buộc, tối đa 20 ký tự|Mới / Đang xử lý / Đã xử lý / Thư rác
FIELD: replyNote|String|Tối đa 1000 ký tự|Nội dung đã trả lời khách
FIELD: handledAt|LocalDateTime|Có thể để trống|
REL: tour|Tour|N-1, tuỳ chọn|Tour đang được hỏi, để trống nếu hỏi chung
REL: handledBy|User|N-1, tuỳ chọn|Nhân viên đã tiếp nhận
METHOD: resolve(User, String)|void|Đổi trạng thái Đã xử lý, ghi người xử lý và mốc thời gian
METHOD: isNew()|boolean|Kiểm tra liên hệ có đang ở trạng thái Mới không
METHOD: Getter/Setter|-|Theo ràng buộc trên
>>END

### 3.2.16 Thực thể PasswordResetToken

>>ENTITY
NAME: PasswordResetToken
TABLE: password_reset_tokens
DESC: Vé một lần dùng để đặt lại mật khẩu. Ba quy tắc an toàn (chưa dùng, chưa hết hạn, vô hiệu hoá vé cũ khi có yêu cầu mới) được kiểm tra khi sử dụng. **Đây là bảng "chết" duy nhất còn lại** trong số bốn bảng mở rộng ban đầu chỉ tồn tại để phục vụ quy tắc chặn xoá: chưa có chức năng "quên mật khẩu" nào trong ứng dụng ghi vào bảng này. Đây là chủ đích trong phạm vi đồ án (việc gửi thư xác nhận hiện chỉ mô phỏng, chưa gửi email thật nên chưa có kênh gửi liên kết đặt lại mật khẩu), không phải sai sót bị bỏ sót.
FIELD: id|Long|Khoá chính, tự tăng|
FIELD: token|String|Bắt buộc, duy nhất, tối đa 100 ký tự|Chuỗi ngẫu nhiên khó đoán
FIELD: expiresAt|LocalDateTime|Bắt buộc|Mặc định 30 phút kể từ lúc tạo
FIELD: usedAt|LocalDateTime|Có thể để trống|Để trống nghĩa là chưa dùng
REL: user|User|N-1, bắt buộc|Chủ vé
METHOD: isExpired()|boolean|So với thời điểm hiện tại
METHOD: isUsed()|boolean|Kiểm tra vé đã được dùng chưa
METHOD: isUsable()|boolean|Chưa dùng và chưa hết hạn
METHOD: markUsed()|void|Ghi nhận thời điểm đã dùng
METHOD: Getter/Setter|-|Theo ràng buộc trên
>>END

### 3.2.17 Cart (đối tượng phiên làm việc)

>>ENTITY
NAME: Cart
TABLE: (không có bảng - giữ tạm trong phiên làm việc của người dùng)
DESC: Giỏ hàng của một phiên làm việc. Không có bảng dữ liệu tương ứng trong cơ sở dữ liệu. Các dòng được lưu theo đợt khởi hành để vừa gộp được dòng trùng, vừa giữ nguyên thứ tự người dùng đã thêm vào giỏ.
FIELD: items|Danh sách CartItem|Khoá theo đợt khởi hành|Không truy cập trực tiếp, chỉ qua các phương thức
METHOD: addOrMerge(CartItem)|void|Thêm dòng mới, hoặc cộng dồn số khách nếu đợt khởi hành đã có trong giỏ
METHOD: remove(Long departureId)|void|Xoá một dòng theo mã đợt khởi hành
METHOD: clear()|void|Làm rỗng giỏ hàng
METHOD: getItemCount()|int|Số dòng trong giỏ, hiện trên huy hiệu điều hướng
METHOD: getTotalQuantity()|int|Tổng số khách của cả giỏ
METHOD: getTotalAmount()|BigDecimal|Tổng tiền của cả giỏ
>>END

### 3.2.18 CartItem (đối tượng phiên làm việc)

>>ENTITY
NAME: CartItem
TABLE: (không có bảng - giữ tạm trong phiên làm việc của người dùng)
DESC: Một dòng trong giỏ hàng: đợt khởi hành nào, mấy người lớn, mấy trẻ em. Mọi thông tin hiển thị (tên tour, ảnh, giá) được chép sẵn vào đây lúc thêm vào giỏ để trang giỏ hàng hiển thị được ngay mà không cần tra cứu lại; giá thật vẫn được đọc lại từ cơ sở dữ liệu ở bước thanh toán để đảm bảo chính xác.
FIELD: departureId|Long|Khoá của dòng trong giỏ|Trùng đợt khởi hành thì gộp thay vì thêm dòng mới
FIELD: tourId / tourCode / tourName / tourSlug / destination / thumbnail|Long/String|Bản sao dữ liệu tour|
FIELD: departureDate / returnDate|LocalDate|Bản sao dữ liệu đợt khởi hành|
FIELD: durationDays / durationNights|Integer|Bản sao dữ liệu tour|
FIELD: priceAdult / priceChild|BigDecimal|Bản sao giá tại thời điểm thêm vào giỏ|Giá thật được đọc lại ở bước thanh toán
FIELD: numAdults / numChildren|int|Số khách của dòng|
METHOD: getQuantity()|int|Tổng số khách của dòng; bằng 0 thì dòng bị xoá khỏi giỏ
METHOD: getSubtotal()|BigDecimal|Thành tiền của dòng
METHOD: merge(int, int)|void|Cộng thêm khách khi đặt trùng đợt khởi hành đã có trong giỏ
METHOD: getDurationText()|String|Trả về chuỗi "X ngày Y đêm"
>>END

## 3.3 Screen Flow: Phân luồng màn hình của ứng dụng

[[HINH: screen-flow | Sơ đồ phân luồng màn hình]]

Luồng màn hình chính của khách vãng lai/khách hàng: **Trang chủ** → **Danh sách tour** (tìm kiếm/lọc) → **Chi tiết tour** (chọn đợt khởi hành) → **Giỏ hàng** → (nếu chưa đăng nhập thì rẽ qua **Đăng nhập/Đăng ký**) → **Thanh toán** (áp mã giảm giá tuỳ chọn) → **Xác nhận đặt tour thành công**. Song song đó, từ menu tài khoản có các nhánh **Hồ sơ cá nhân**, **Đổi mật khẩu**, **Lịch sử đơn đặt** → **Chi tiết đơn**.

Luồng màn hình của quản trị viên: **Đăng nhập** → **Trang tổng quan** (số liệu + biểu đồ doanh thu) → các nhánh **Quản lý danh mục**, **Quản lý tour** → **Quản lý đợt khởi hành** (của một tour cụ thể), **Quản lý đơn đặt tour** → **Chi tiết đơn**, **Quản lý tài khoản**, **Quản lý khuyến mãi**. Mọi màn hình danh sách trong khu quản trị đều có nhánh phụ **Biểu mẫu thêm/sửa** dùng chung cho cả hai thao tác.

## 3.4 Cơ sở dữ liệu

### 3.4.1 Cơ sở dữ liệu quan hệ

Sơ đồ thực thể và mối kết hợp (ERD):

[[HINH: 03. Biểu đồ ERD - TourBookingDB]]

Sơ đồ trên máy chủ CSDL (Database Diagram):

[[HINH: 03b. Biểu đồ Database Diagram - TourBookingDB]]

Hai sơ đồ trên (Hình 3.4.1.1 và 3.4.1.2) đã được vẽ lại từ chính tệp `database/02_schema.sql` bằng `docs/report/diagrams/src/ERD.py` và `DatabaseDiagram.py`, đủ 17 bảng và 21 khoá ngoại, kể cả bảng `user_events`. Bảng này không có khoá ngoại nên ở ERD được vẽ riêng kèm ghi chú (`user_id`, `tour_id`, `departure_id` chỉ là tham chiếu logic, không vẽ đường nối để không gợi ý một ràng buộc không tồn tại), còn ở Database Diagram không có đường nối nào. Bố cục (hộp, đường nối, nhãn) được `check_layout.py` kiểm tra hình học: không đường nào xuyên hộp, không hai đường chồng đoạn, không nhãn nào đè, và 0 điểm cắt nhau.

Cơ sở dữ liệu của hệ thống chạy trên Microsoft SQL Server, có hỗ trợ tiếng Việt, gồm đúng **17 bảng**: `users`, `tour_categories`, `tours`, `tour_images`, `tour_departures`, `tour_itineraries`, `bookings`, `booking_details`, `booking_passengers`, `booking_status_history`, `payments`, `promotions`, `coupon_usages`, `reviews`, `contact_messages`, `password_reset_tokens`, `user_events`. Cấu trúc các bảng được thiết kế và kiểm soát thủ công theo đúng mô hình lớp thực thể ở mục 3.1-3.2, đảm bảo chương trình chỉ khởi động được khi cấu trúc dữ liệu khớp đúng với thiết kế.

### 3.4.2 Các ràng buộc toàn vẹn trong CSDL

**Về toàn vẹn thực thể:** mỗi bảng có khoá chính tự tăng, không được để trống, không trùng lặp. Các mã nghiệp vụ hiển thị cho người dùng (mã tour, mã đơn, mã khuyến mãi, đường dẫn danh mục, đường dẫn tour, email đăng nhập) đều bắt buộc phải duy nhất trong toàn hệ thống.

**Về toàn vẹn tham chiếu:** mọi bảng con đều tham chiếu đúng khoá chính của bảng cha. Một bản ghi cha đang được bảng con tham chiếu thì không xoá được (đây là **lớp bảo vệ cuối cùng**); tuy nhiên, thông báo cho người dùng khi chặn xoá không phải là một lỗi kỹ thuật khó hiểu, mà là một câu tiếng Việt rõ ràng do chương trình chủ động kiểm tra và báo trước (xem bảng dưới).

**Về toàn vẹn miền giá trị:** các trường dữ liệu đều tuân theo kiểu dữ liệu và phạm vi hợp lệ, được chương trình kiểm tra ngay khi người dùng nhập liệu, không phó mặc cho ràng buộc của cơ sở dữ liệu:

- Số điện thoại: đúng định dạng số điện thoại Việt Nam - bắt đầu bằng số 0, tổng cộng 10 hoặc 11 chữ số.
- Email: đúng định dạng email chuẩn, tối đa 150 ký tự, và khi đăng ký phải là email chưa từng được dùng.
- Mật khẩu: tối thiểu 6, tối đa 50 ký tự; mật khẩu và xác nhận mật khẩu phải trùng khớp.
- Số tiền (giá tour, giá người lớn/trẻ em, tổng tiền, số tiền giảm giá...): luôn tính toán chính xác tuyệt đối, không làm tròn sai lệch khi cộng dồn nhiều dòng.
- Số chỗ (tổng số chỗ, số chỗ còn trống): luôn là số nguyên dương, không bao giờ được phép âm.
- Số sao đánh giá: khoảng giá trị 1-5.

**Về toàn vẹn nghiệp vụ - đặc thù quan trọng nhất của đồ án:** đề bài yêu cầu không được đặt bất kỳ ràng buộc, hàm tính toán, thủ tục lưu trữ hay cơ chế tự động kích hoạt nào ở tầng cơ sở dữ liệu. Toàn bộ quy tắc nghiệp vụ dưới đây được chương trình ứng dụng chủ động kiểm tra và từ chối kèm thông báo rõ ràng khi vi phạm:

| Xoá | Bị chặn khi | Thông báo cho người dùng |
|---|---|---|
| Danh mục tour | còn tour thuộc danh mục | "Không thể xoá danh mục còn tour" |
| Tour | đã có lượt đặt / đã có đánh giá / đã có liên hệ | Thông báo tương ứng từng trường hợp, ví dụ "Không xoá được: còn liên hệ đang hỏi về tour này" |
| Đợt khởi hành | đã có lượt đặt | "Không thể xoá đợt khởi hành đã có khách đặt" |
| Tài khoản | có đơn / là chính mình / là quản trị viên cuối cùng / có đánh giá / có lượt dùng mã / có lịch sử xử lý đơn hoặc liên hệ | Thông báo tương ứng từng trường hợp, ví dụ "Không xoá được: tài khoản này đã từng đổi trạng thái đơn hàng" |

Kết quả kiểm chứng thật trên cơ sở dữ liệu đang chạy: số lượng ràng buộc kiểm tra dữ liệu, hàm tính toán, thủ tục lưu trữ và cơ chế tự động kích hoạt đặt ở tầng cơ sở dữ liệu đều bằng **0** trên toàn bộ hệ thống.

## 3.5 Một số màn hình thiết kế

### 3.5.1 Trang chủ

[[HINH: screenshot-trang-chu | Màn hình trang chủ]]

Chức năng của màn hình: giới thiệu tổng quan hệ thống, dẫn dắt người dùng tới tìm kiếm và đặt tour. Mô tả: banner giới thiệu kèm ô tìm kiếm nhanh, dải tour nổi bật lấy trực tiếp từ dữ liệu (đánh dấu `featured`), khối số liệu ấn tượng (số tour, số điểm đến), khối danh mục tour, dải địa danh chạy ngang, khối "bốn bước đặt tour".

### 3.5.2 Trang tìm kiếm và danh sách tour

[[HINH: screenshot-danh-sach-tour | Màn hình tìm kiếm và danh sách tour]]

Chức năng của màn hình: cho phép tìm kiếm theo từ khoá và lọc theo danh mục/khoảng giá. Mô tả: thanh tìm kiếm và bộ lọc ở đầu trang, lưới thẻ tour bên dưới kèm ảnh hoặc khung cảnh minh hoạ, giá "từ...", số ngày/đêm; điều khiển phân trang giữ nguyên toàn bộ bộ lọc đang áp dụng.

### 3.5.3 Trang chi tiết tour

[[HINH: screenshot-chi-tiet-tour | Màn hình chi tiết tour]]

Chức năng của màn hình: cung cấp đầy đủ thông tin để khách quyết định đặt. Mô tả: thư viện ảnh, mô tả tour, lịch trình từng ngày dạng gấp mở, bảng các đợt khởi hành kèm ngày đi/về, giá người lớn/trẻ em, số chỗ còn trống và nút "Thêm vào giỏ" cho từng đợt.

### 3.5.4 Trang giỏ hàng và thanh toán

[[HINH: screenshot-thanh-toan | Màn hình giỏ hàng và thanh toán]]

Chức năng của màn hình: hoàn tất việc đặt tour. Mô tả: danh sách các dòng trong giỏ kèm ảnh, tên tour, ngày đi, số khách, thành tiền; biểu mẫu thông tin liên hệ được điền sẵn; ô nhập mã giảm giá kiểm bằng AJAX; tổng tiền cập nhật theo thời gian thực khi áp mã.

### 3.5.5 Trang tổng quan khu quản trị

[[HINH: screenshot-admin-dashboard | Màn hình tổng quan khu quản trị]]

Chức năng của màn hình: giúp quản trị viên nắm nhanh tình hình kinh doanh. Mô tả: các thẻ số liệu khái quát (đơn theo trạng thái, tour đang bán, tài khoản), biểu đồ cột doanh thu theo tháng vẽ bằng SVG đọc dữ liệu qua `/api/admin/stats/revenue`, có đường kẻ mốc và nhãn số tiền.

Lưu ý khi đọc ảnh chụp màn hình này: ở bản chụp tĩnh đính kèm báo cáo, khối biểu đồ doanh thu có thể hiện trống. Đây **không phải lỗi tính năng** - biểu đồ được vẽ bằng JavaScript sau khi gọi AJAX tới `/api/admin/stats/revenue`, mà công cụ chụp ảnh tĩnh không mang theo cookie phiên đăng nhập nên lệnh gọi bị từ chối và script vẽ biểu đồ không có dữ liệu để chạy. Khi mở bằng trình duyệt thật với phiên đăng nhập quản trị viên hợp lệ, biểu đồ hiển thị đầy đủ.

### 3.5.6 Trang quản lý tour (khu quản trị)

[[HINH: screenshot-admin-tour | Màn hình quản lý tour]]

Chức năng của màn hình: quản trị viên thêm/sửa tour và ảnh minh hoạ. Mô tả: danh sách tour có phân trang và bộ lọc; biểu mẫu thêm/sửa gồm thông tin chung, chọn danh mục, nhập lịch trình từng ngày, khu vực kéo-thả nhiều ảnh để tải lên; nút "Xoá" chỉ khả dụng khi tour chưa có lượt đặt/đánh giá/liên hệ.

### 3.5.7 Trang kiểm duyệt đánh giá (khu quản trị, UC020)

[[HINH: screenshot-admin-review | Màn hình kiểm duyệt đánh giá]]

Chức năng của màn hình: cho quản trị viên duyệt/bỏ duyệt/trả lời/xoá đánh giá của khách hàng trước khi đánh giá đó được phép hiển thị công khai ở trang chi tiết tour. Mô tả: danh sách phân trang tại `/admin/reviews`, mặc định lọc theo trạng thái "Chờ duyệt" (có thể chuyển sang "Đã duyệt"/"Tất cả"); mỗi dòng hiện tour, người viết, số sao, trích đoạn nội dung, ngày gửi, và nhãn "Đã xác thực" khi đánh giá gắn với một đơn đã hoàn thành thật; nút "Duyệt"/"Bỏ duyệt" đảo cờ `approved`, nút "Trả lời" ghi `admin_reply`, nút "Xoá" xoá hẳn bản ghi; menu quản trị có huy hiệu đếm số đánh giá đang chờ duyệt.

### 3.5.8 Trang gửi liên hệ (UC002)

[[HINH: screenshot-contact | Màn hình gửi liên hệ]]

Chức năng của màn hình: cho phép bất kỳ ai gửi câu hỏi hoặc yêu cầu tư vấn, không cần đăng nhập. Mô tả: biểu mẫu tại `/contact` gồm họ tên, email, số điện thoại (tuỳ chọn), tiêu đề, nội dung; khi mở từ nút "Liên hệ về tour này" ở trang chi tiết tour thì tên tour đang hỏi hiện sẵn phía trên biểu mẫu và được gắn kèm vào liên hệ gửi đi.

### 3.5.9 Trang quản lý liên hệ (khu quản trị, UC021)

[[HINH: screenshot-admin-contact | Màn hình quản lý liên hệ]]

Chức năng của màn hình: cho quản trị viên xử lý các liên hệ khách gửi từ chức năng Gửi liên hệ. Mô tả: danh sách phân trang, mặc định lọc "Mới"; mỗi dòng hiện người gửi, tour đang hỏi (nếu có), tiêu đề, ngày gửi; xem chi tiết và trả lời ngay trong bảng theo kiểu gấp mở, cùng khuôn với màn kiểm duyệt đánh giá; đổi trạng thái sang "Đã xử lý"/"Thư rác" thì ghi lại người xử lý và thời điểm xử lý; menu quản trị có huy hiệu đếm số liên hệ "Mới".

### 3.5.10 Bằng chứng ràng buộc xoá được kiểm tra trong chương trình

[[HINH: screenshot-409 | Thông báo hệ thống từ chối xoá dữ liệu đang được tham chiếu]]

Đây là bằng chứng trực quan gọn nhất cho một luận điểm xuyên suốt đồ án: **mọi ràng buộc chặn xoá đều được chương trình ứng dụng chủ động kiểm tra, không phải là một ràng buộc khoá ngoại trần trụi của cơ sở dữ liệu.** Ảnh chụp phản hồi thật khi thử xoá một tour đang còn liên hệ đang hỏi thăm: hệ thống từ chối thao tác và trả về một câu tiếng Việt đầy đủ, dễ hiểu ("Không xoá được: còn 1 liên hệ đang hỏi về tour này.") - khác hẳn một thông báo lỗi kỹ thuật khó hiểu kiểu "thao tác xoá xung đột với ràng buộc tham chiếu...". Đây đúng là điểm nhấn quan trọng của đồ án mà trước nay báo cáo chưa từng có ảnh chụp minh hoạ thật.

# CHƯƠNG 4: KIỂM THỬ

## 4.1 Những yêu cầu về tài nguyên cho kiểm thử ứng dụng

### Phần cứng

| CPU | RAM | HDD/SSD | Kết nối |
|---|---|---|---|
| Intel Core i5 trở lên, 2.0 GHz | 8 GB trở lên | Còn trống tối thiểu 2 GB | Không bắt buộc mạng ngoài (SQL Server chạy cục bộ) |

### Phần mềm

| Tên phần mềm | Phiên bản | Loại |
|---|---|---|
| Amazon Corretto (OpenJDK) | 23 (biên dịch `--release 21`) | JDK |
| Apache Maven Wrapper (`mvnw`) | đi kèm dự án | Công cụ build |
| Microsoft SQL Server | 2025 Developer Edition | Hệ quản trị cơ sở dữ liệu |
| SQLCMD (ODBC Driver 18 Tools) | 18.x | Công cụ dòng lệnh SQL Server |
| Microsoft Windows | 10/11 64-bit | Hệ điều hành |
| Trình duyệt (Chrome/Edge) | Bản mới nhất | Kiểm thử giao diện |

## 4.2 Danh sách các tình huống để kiểm tra ứng dụng

| Test ID | Chức năng | Mô tả | Điều kiện trước | Dữ liệu Test | Kết quả mong muốn | Ghi chú |
|---|---|---|---|---|---|---|
| TS01 | UC005 Đăng nhập | Đăng nhập tài khoản khách hàng hợp lệ | Tài khoản mẫu đã có sẵn | Một tài khoản khách hàng mẫu với mật khẩu đúng | Đăng nhập thành công, chuyển về trang chủ/trang trước đó |  |
| TS02 | UC005 Đăng nhập | Sai mật khẩu | Tài khoản tồn tại | Tài khoản mẫu, nhập sai mật khẩu | Thông báo lỗi, không đăng nhập, ở lại trang đăng nhập |  |
| TS03 | UC004+UC005 Giỏ hàng sống sót qua đăng nhập | Thêm 1 dòng vào giỏ lúc chưa đăng nhập, sau đó đăng nhập | Chưa đăng nhập, có ít nhất 1 đợt khởi hành còn chỗ | Thêm 2 người lớn vào một đợt khởi hành, sau đó đăng nhập bằng tài khoản mẫu | Giỏ hàng vẫn còn nguyên dòng vừa thêm sau khi đăng nhập thành công |  |
| TS04 | UC001 Tìm kiếm tour không dấu | Gõ từ khoá không dấu để tìm tour có dấu | Có tour tên chứa "Đà Lạt" | Từ khoá lần lượt: "da lat", "DA LAT", "Đà Lạt" | Cả ba lần tìm đều trả về cùng một tour Đà Lạt |  |
| TS05 | UC006 Đặt tour kèm mã giảm giá | Đặt tour, áp mã giảm giá bị chặn trần | Đã đăng nhập, giỏ hàng có tổng 5.990.000đ, có mã giảm 10% kèm trần giảm | Áp mã vào đơn 5.990.000đ | Số tiền giảm bị chặn ở mức trần 500.000đ (không phải 10% = 599.000đ), tổng thanh toán còn 5.490.000đ |  |
| TS06 | UC007 Dùng lại mã giảm giá đã dùng | Cùng tài khoản dùng lại đúng mã đã áp thành công ở TS05 | Đã đặt thành công 1 đơn dùng mã đó | Dùng lại đúng mã đó bằng chính tài khoản đã dùng | Bị từ chối, thông báo đã dùng mã này rồi |  |
| TS07 | UC015 Chống bán quá chỗ | Hai yêu cầu đặt cùng lúc vào một đợt khởi hành chỉ còn ít chỗ | Đợt khởi hành còn ít hơn tổng số khách của hai yêu cầu cộng lại | Hai phiên đặt cùng lúc, mỗi phiên đặt số khách gần bằng số chỗ còn lại | Chỉ một yêu cầu thành công; yêu cầu còn lại nhận lỗi "không đủ chỗ", số chỗ còn trống không bao giờ âm |  |
| TS08 | UC014 Chặn xoá tour đã có đánh giá | Xoá một tour đã có đánh giá | Tour đã có ít nhất 1 đánh giá | Xoá tour đó trong khu quản trị | Hệ thống từ chối xoá, thông báo không thể xoá tour đã có đánh giá |  |
| TS09 | UC014 Xoá tour sạch dữ liệu | Xoá một tour chưa từng phát sinh giao dịch | Tour chưa có lượt đặt/đánh giá/liên hệ nào | Xoá tour đó trong khu quản trị | Tour và toàn bộ ảnh liên quan bị xoá thành công |  |
| TS10 | UC014 Chặn tải ảnh không hợp lệ | Tải lên một tệp văn bản thay vì ảnh khi thêm tour | Đã đăng nhập quản trị viên | Chọn một tệp không phải ảnh ở biểu mẫu thêm tour | Không lưu bất kỳ ảnh hay thông tin nào của tour, hiển thị thông báo lỗi định dạng |  |
| TS11 | UC016 Huỷ đơn hoàn lại chỗ | Huỷ một đơn đang ở trạng thái chờ xác nhận | Đơn tồn tại, đợt khởi hành liên quan đã bị trừ đúng số chỗ của đơn | Đổi trạng thái đơn sang "Đã huỷ" | Trạng thái đơn đổi thành "Đã huỷ", số chỗ còn trống của đợt khởi hành tăng lại đúng bằng số khách của đơn |  |
| TS12 | UC009 Kiểm tra quyền sở hữu đơn | Một khách hàng cố mở đơn của khách hàng khác | Hai tài khoản khách hàng khác nhau, mỗi người có ít nhất một đơn | Đăng nhập tài khoản A, gõ thẳng địa chỉ chi tiết đơn của tài khoản B | Bị từ chối truy cập, không hiển thị thông tin đơn của B |  |
| TS13 | UC017 Chặn tự xoá chính mình | Quản trị viên đang đăng nhập cố xoá chính tài khoản của mình | Đã đăng nhập quản trị viên | Tự xoá chính tài khoản đang đăng nhập trong khu quản trị | Bị từ chối, thông báo "Không thể tự xoá chính mình" |  |
| TS14 | UC017 Chặn xoá quản trị viên cuối cùng | Xoá quản trị viên duy nhất còn hoạt động | Chỉ còn đúng một tài khoản quản trị viên "sạch" đang hoạt động (chưa từng đổi trạng thái đơn/xử lý liên hệ/duyệt đánh giá) | Xoá tài khoản quản trị viên đó bằng một tài khoản quản trị viên khác | Bị từ chối, thông báo "Không thể xoá quản trị viên cuối cùng" | **Chưa kiểm chứng đúng kịch bản** - tài khoản quản trị viên thứ hai hiện có đã từng đổi trạng thái đơn (xem TS23) nên bị chặn bởi quy tắc khác trước (xem TS25), chưa kịp chạm tới quy tắc "quản trị viên cuối cùng"; cần một tài khoản quản trị viên thứ ba hoàn toàn sạch mới tách riêng được tình huống này |
| TS15 | UC013 Chặn xoá danh mục còn tour | Xoá một danh mục đang còn tour | Danh mục có ít nhất 1 tour thuộc về nó | Xoá danh mục đó trong khu quản trị | Bị từ chối, thông báo "Không thể xoá danh mục còn tour" |  |
| TS16 | Bảo mật chống giả mạo yêu cầu | Gửi yêu cầu thay đổi dữ liệu không đi từ biểu mẫu hợp lệ của hệ thống | Chưa đăng nhập | Gửi thẳng yêu cầu thêm vào giỏ hàng mà không qua biểu mẫu của trang | Bị điều hướng về trang đăng nhập thay vì được thực hiện, đúng hành vi bảo vệ khi người dùng chưa xác thực |  |
| TS17 | Không lộ mật khẩu đã mã hoá | Kiểm tra mọi trang trả về không chứa mật khẩu đã mã hoá của người dùng | Đã đăng nhập | Duyệt qua các trang tài khoản, trang quản trị | Không trang nào lộ mật khẩu đã mã hoá của người dùng |  |
| TS18 | UC010 Đánh giá tour đã đi | Khách hàng gửi đánh giá cho tour đã hoàn thành; đánh giá phải ở trạng thái chờ duyệt, KHÔNG hiện công khai ngay | Tạm chuyển một đơn sang trạng thái hoàn thành, khách hàng chưa từng đánh giá tour trong đơn đó | Gửi đánh giá cho tour trong đơn vừa chuyển | Đánh giá được ghi nhận ở trạng thái chờ duyệt; khách thấy ngay đánh giá của mình kèm nhãn "Chờ duyệt"; khách vãng lai xem cùng trang chi tiết tour đếm được 0 đánh giá | Đúng như mong đợi: khách gửi xong thấy nhãn "Chờ duyệt" ở trang cá nhân; khách vãng lai xem trang chi tiết tour không thấy đánh giá nào (đếm 0). Dữ liệu kiểm thử đã dọn sạch, đơn và đánh giá trả về trạng thái ban đầu sau khi kiểm |
| TS19 | UC018 Quản lý mã khuyến mãi (phía quản trị) | Xoá một mã khuyến mãi đã có lượt dùng, và xoá một mã sạch | Một mã đã có ít nhất 1 lượt dùng; có sẵn một mã khác chưa ai dùng | Xoá lần lượt hai mã đó trong khu quản trị | Mã đã dùng bị chặn xoá kèm câu tiếng Việt rõ ràng; mã sạch xoá được | Mã đã dùng bị chặn kèm câu "Không xoá được: mã này đã có 1 lượt dùng. Hãy dùng Vô hiệu hoá mã…"; mã sạch xoá thành công |
| TS20 | UC020 Kiểm duyệt đánh giá | Luồng đầy đủ trên đánh giá có sẵn (Nguyễn Văn An, tour Huế, 3 sao, đang chờ duyệt): duyệt → hiện ra + điểm trung bình đổi; bỏ duyệt → biến mất lại; trả lời; xoá | Có sẵn 1 đánh giá đang chờ duyệt cho tour đó, trang chi tiết tour trước đó báo "Chưa có đánh giá" | Tại màn kiểm duyệt, lọc "Chờ duyệt", lần lượt bấm Duyệt, Bỏ duyệt, Trả lời, Xoá trên đúng đánh giá đó | Duyệt: đánh giá hiện ngay, điểm trung bình đổi. Bỏ duyệt: biến mất lại, huy hiệu chờ duyệt tăng về 1. Trả lời: lưu đúng nội dung phản hồi. Xoá: xoá thật, huy hiệu giảm theo | Đúng như mong đợi, đo được số cụ thể: trước khi duyệt trang tour không hiện đánh giá nào ("Chưa có đánh giá"); sau khi Duyệt, điểm trung bình đổi từ "Chưa có đánh giá" thành **3,0**; Bỏ duyệt/Trả lời/Xoá đều đúng như thiết kế |
| TS21 | UC002+UC014 Chặn xoá tour đang có liên hệ | Gửi liên hệ về một tour cụ thể, sau đó thử xoá tour đó | Tour cần thử xoá tồn tại | Gửi liên hệ về tour đó, sau đó xoá tour đó trong khu quản trị | Bị hệ thống từ chối xoá, thông báo nêu rõ số liên hệ đang hỏi | Bị chặn kèm câu "Không xoá được: còn 1 liên hệ đang hỏi về tour này." |
| TS22 | UC006 Đặt tour sinh hồ sơ thanh toán + mốc lịch sử trạng thái | Đặt tour thật, kiểm tra hồ sơ thanh toán và dòng lịch sử trạng thái sau khi đặt | Đã đăng nhập, giỏ hàng hợp lệ | Hoàn tất đặt 1 đơn | Sinh đúng 1 hồ sơ thanh toán ở trạng thái "Chờ thanh toán" (đúng số tiền) và đúng 1 mốc lịch sử trạng thái đầu tiên (chuyển sang "Chờ xác nhận") | Đúng như mong đợi: đúng 1 hồ sơ thanh toán khớp số tiền đơn, đúng 1 mốc lịch sử trạng thái đầu tiên |
| TS23 | UC016 Đổi trạng thái đơn ghi lịch sử | Đổi trạng thái đơn 2 lần liên tiếp, mỗi lần kèm lý do | Đơn tồn tại ở trạng thái "Chờ xác nhận" | Đổi "Chờ xác nhận"→"Đã xác nhận" (kèm lý do), rồi "Đã xác nhận"→"Hoàn thành" (kèm lý do khác) | Dòng thời gian của đơn có thêm đúng 2 mốc mới, mỗi mốc đúng lý do đã nhập | Đúng như mong đợi: dòng thời gian dài thêm đúng 2 mốc kèm lý do |
| TS24 | UC016 Chặn trùng mã giao dịch khi đánh dấu đã thu tiền | Đánh dấu đã thu tiền bằng mã giao dịch trùng với một khoản thanh toán khác đã có | Đã có 1 khoản thanh toán hoàn tất với một mã giao dịch cụ thể | Đánh dấu đã thu tiền cho một khoản khác, nhập đúng mã giao dịch đã dùng ở trên | Bị chặn, hiển thị lỗi tiếng Việt, khoản đã hoàn tất trước đó không bị ghi đè | Đúng như mong đợi: bị chặn kèm thông báo lỗi tiếng Việt, khoản cũ giữ nguyên |
| TS25 | UC017 Chặn xoá tài khoản quản trị viên đã đổi trạng thái đơn | Xoá tài khoản quản trị viên đã đổi trạng thái đơn 2 lần ở TS23 | Tài khoản đó đã thao tác đổi trạng thái đơn ở TS23 | Xoá tài khoản đó trong khu quản trị | Bị chặn kèm câu tiếng Việt nêu số lần đổi trạng thái | Bị chặn kèm câu "Không xoá được: tài khoản này đã 2 lần đổi trạng thái đơn hàng…" - lần đầu tiên quy tắc này kích hoạt bằng dữ liệu không phải dữ liệu mẫu có sẵn |

## 4.3 Báo cáo kết quả kiểm thử (Testcase report)

| Báo cáo kết quả kiểm thử |  |  |
|---|---|---|
| Người thực hiện: nhóm đồ án TourBooking | Passed: | 23 |
| Môn học: Lập trình WWW (Java) - IUH | Chưa kiểm được: | 2 |
|  | Tổng số test case: | 25 |

| STT | Test Case ID | Chức năng | Dữ liệu đầu vào | Kết quả mong muốn | Kết quả thực tế | Trạng thái | Ghi chú |
|---|---|---|---|---|---|---|---|
| 1 | TS01 | Đăng nhập | Tài khoản khách hàng mẫu, mật khẩu đúng | Đăng nhập thành công | Phân quyền đúng theo vai trò, chuyển hướng đúng chỗ | Pass |  |
| 2 | TS02 | Đăng nhập sai mật khẩu | Tài khoản mẫu, mật khẩu sai | Thông báo lỗi, không đăng nhập | Không trang nào lộ mật khẩu đã mã hoá, hành vi từ chối đúng như thiết kế | Pass |  |
| 3 | TS03 | Giỏ hàng sống sót qua đăng nhập | Thêm giỏ rồi đăng nhập | Giỏ hàng còn nguyên sau đăng nhập | Xác nhận giỏ hàng sống sót qua đăng nhập | Pass |  |
| 4 | TS04 | Tìm kiếm không dấu | "da lat" / "DA LAT" / "Đà Lạt" | Cùng ra 1 kết quả | Cả ba cách gõ đều tìm ra đúng 1 tour Đà Lạt | Pass |  |
| 5 | TS05 | Đặt tour + áp mã giảm giá bị chặn trần | Đơn 5.990.000đ, mã giảm 10% có trần | Giảm 500.000đ (10% bị chặn trần), còn 5.490.000đ | Đặt đơn thật: 5.990.000 → giảm 500.000 → 5.490.000, ghi nhận đúng 1 lượt dùng mã | Pass |  |
| 6 | TS06 | Dùng lại mã đã dùng | Dùng lại đúng mã đó bằng cùng tài khoản | Bị từ chối | Dùng lại mã bằng chính tài khoản đó bị từ chối, đúng như thiết kế | Pass |  |
| 7 | TS07 | Chống bán quá chỗ | Hai yêu cầu đặt cùng lúc | Chỉ một thành công, không âm chỗ | Cơ chế kiểm soát phiên bản đã cài đặt từ đầu; hành vi hai yêu cầu cạnh tranh **chưa đo lại bằng công cụ giả lập tải đồng thời thật** trong lần kiểm gần nhất | Chưa kiểm | Cần công cụ giả lập nhiều luồng gửi cùng lúc để đo trực tiếp |
| 8 | TS08 | Chặn xoá tour đã có đánh giá | Xoá một tour đã có đánh giá | Bị từ chối xoá | Hệ thống từ chối xoá, đúng như thiết kế | Pass |  |
| 9 | TS09 | Xoá tour sạch dữ liệu | Xoá một tour chưa có giao dịch | Xoá thành công | Tour và ảnh liên quan bị xoá đúng như thiết kế | Pass |  |
| 10 | TS10 | Chặn tải ảnh không hợp lệ | Tải một tệp không phải ảnh | Không lưu ảnh, báo lỗi | Tệp bị chặn, không lưu ảnh nào, không lưu thông tin tour | Pass |  |
| 11 | TS11 | Huỷ đơn hoàn lại chỗ | Đổi trạng thái đơn sang "Đã huỷ" | Chỗ được hoàn lại | Huỷ đơn thì chỗ trả về đúng, đã xác nhận bằng thao tác thật | Pass |  |
| 12 | TS12 | Kiểm tra quyền sở hữu đơn | Tài khoản A mở đơn của B | Bị từ chối | Khách khác mở đơn của người khác bị từ chối truy cập | Pass |  |
| 13 | TS13 | Chặn tự xoá chính mình | Quản trị viên tự xoá mình | Bị từ chối | Quy tắc chặn xoá chạy đúng kèm thông báo tiếng Việt | Pass |  |
| 14 | TS14 | Chặn xoá quản trị viên cuối cùng | Xoá quản trị viên cuối cùng | Bị từ chối | Chưa đúng kịch bản thật: tài khoản quản trị viên thứ hai hiện có đã bị một quy tắc khác chặn trước (xem TS25), chưa kịp chạm quy tắc "quản trị viên cuối cùng" | Chưa kiểm | Cần một tài khoản quản trị viên thứ ba hoàn toàn sạch mới tách riêng được tình huống này |
| 15 | TS15 | Chặn xoá danh mục còn tour | Xoá danh mục có tour | Bị từ chối | Quy tắc chặn xoá chạy đúng kèm thông báo tiếng Việt | Pass |  |
| 16 | TS16 | Bảo mật chống giả mạo yêu cầu | Gửi yêu cầu không qua biểu mẫu hợp lệ, chưa đăng nhập | Bị điều hướng về trang đăng nhập | Bị chặn đúng như thiết kế bảo vệ khi chưa xác thực | Pass |  |
| 17 | TS17 | Không lộ mật khẩu đã mã hoá | Duyệt các trang sau đăng nhập | Không trang nào lộ mật khẩu | Xác nhận không trang nào lộ mật khẩu đã mã hoá | Pass |  |
| 18 | TS18 | UC010 Đánh giá tour đã đi | Gửi đánh giá cho một đơn tạm chuyển sang hoàn thành | Đánh giá phải ở trạng thái chờ duyệt, chưa hiện ở trang công khai | Khách thấy ngay đánh giá của mình kèm nhãn "Chờ duyệt"; khách vãng lai xem cùng trang đếm được 0 đánh giá; dữ liệu kiểm thử đã dọn sạch sau khi kiểm | Pass |  |
| 19 | TS19 | UC018 Quản lý mã khuyến mãi | Xoá một mã đã có lượt dùng; xoá một mã sạch | Mã đã dùng bị chặn; mã sạch xoá được | Mã đã dùng bị chặn kèm câu "Không xoá được: mã này đã có 1 lượt dùng. Hãy dùng Vô hiệu hoá mã…"; mã sạch xoá thành công | Pass |  |
| 20 | TS20 | UC020 Kiểm duyệt đánh giá | Duyệt/bỏ duyệt/trả lời/xoá đánh giá có sẵn (Nguyễn Văn An, tour Huế, 3 sao) | Duyệt → hiện công khai, điểm trung bình đổi; bỏ duyệt → ẩn lại; trả lời lưu đúng nội dung; xoá xoá thật | Trước khi duyệt, trang tour báo "Chưa có đánh giá"; sau khi Duyệt, điểm trung bình đổi thành **3,0**; Bỏ duyệt → biến mất lại, huy hiệu chờ duyệt tăng về 1; Trả lời lưu đúng nội dung; Xoá xoá thật, huy hiệu giảm theo | Pass |  |
| 21 | TS21 | UC002+UC014 Chặn xoá tour đang có liên hệ | Gửi liên hệ về một tour, rồi xoá tour đó | Bị chặn kèm câu tiếng Việt | Bị chặn kèm câu "Không xoá được: còn 1 liên hệ đang hỏi về tour này." | Pass |  |
| 22 | TS22 | UC006 Đặt tour sinh hồ sơ thanh toán + mốc lịch sử | Đặt 1 đơn thật | Sinh 1 hồ sơ thanh toán "Chờ thanh toán" + 1 mốc lịch sử đầu tiên | Đúng 1 hồ sơ thanh toán khớp số tiền đơn; đúng 1 mốc lịch sử trạng thái đầu tiên | Pass |  |
| 23 | TS23 | UC016 Đổi trạng thái đơn ghi lịch sử | Đổi trạng thái đơn 2 lần liên tiếp kèm lý do | Dòng thời gian có thêm đúng 2 mốc kèm lý do | Dòng thời gian dài thêm đúng 2 mốc, đúng lý do đã nhập mỗi lần | Pass |  |
| 24 | TS24 | UC016 Chặn trùng mã giao dịch | Đánh dấu đã thu tiền bằng mã giao dịch trùng một khoản đã hoàn tất | Bị chặn, khoản cũ không bị ghi đè | Bị chặn kèm thông báo lỗi tiếng Việt; khoản cũ giữ nguyên | Pass |  |
| 25 | TS25 | UC017 Chặn xoá quản trị viên đã đổi trạng thái đơn | Xoá tài khoản quản trị viên đã đổi trạng thái đơn 2 lần ở TS23 | Bị chặn kèm câu tiếng Việt | Bị chặn kèm câu "Không xoá được: tài khoản này đã 2 lần đổi trạng thái đơn hàng…" | Pass | Lần đầu tiên quy tắc này kích hoạt bằng dữ liệu không phải dữ liệu mẫu có sẵn |

Ngoài các tình huống nghiệp vụ trên, các phép đo chất lượng giao diện cũng đã được kiểm chứng bằng công cụ thật trong quá trình phát triển: tương phản màu chữ trên nền đạt chuẩn WCAG ở toàn bộ 12/12 cặp màu được đo (tỉ lệ tối thiểu 4,5:1), không có hiện tượng tràn ngang ở các bề rộng khung nhìn 375px/1024px/1440px, và không có lỗi hiển thị nào khi cuộn qua toàn bộ hiệu ứng hiện dần trên trang.

# CHƯƠNG 5: ỨNG DỤNG TRÍ TUỆ NHÂN TẠO VÀ THEO DÕI HÀNH VI NGƯỜI DÙNG

Chương này trình bày phần mở rộng của đồ án theo chuyên đề trí tuệ nhân tạo (AI) gồm ba nội dung: Spring AI, cơ sở dữ liệu vector và theo dõi hành vi người dùng trên giao diện. Phần lý thuyết bám theo Chương 7 "Spring AI" trong bài giảng của giảng viên; phần còn lại mô tả cách nhóm áp dụng vào TourBooking, từ kiến trúc, cấu hình, các lớp chính, bảng dữ liệu mới, cách chạy cho đến những gì đã kiểm chứng và những gì chưa thể kiểm chứng. Các tính năng AI là tuỳ chọn: khi không có khoá truy cập mô hình, website vẫn chạy đầy đủ như trước.

## 5.1 Cơ sở lý thuyết

### 5.1.1 Spring AI (theo Chương 7 của bài giảng)

Spring AI là dự án thuộc hệ sinh thái Spring giúp đưa các mô hình AI sinh nội dung vào ứng dụng Java theo cách quen thuộc của lập trình viên Spring: cấu hình bằng tệp cấu hình, tiêm phụ thuộc, và đổi nhà cung cấp mô hình chủ yếu bằng cấu hình mà mã Java gần như giữ nguyên. Dự án lấy cảm hứng từ LangChain và LlamaIndex nhưng không phải bản chuyển ngữ của chúng. Các khái niệm chính được dùng trong đồ án gồm:

- ChatClient: giao diện lập trình theo kiểu chuỗi lời gọi (fluent API) để gửi lời nhắc tới mô hình, tương tự RestClient của Spring; có thể gắn sẵn lời nhắc hệ thống, các advisor và các công cụ cho mọi lần gọi.

- PromptTemplate và Structured Output: lời nhắc có biến để tái sử dụng, và việc ánh xạ câu trả lời của mô hình sang đối tượng Java.

- Advisor và Chat Memory: advisor là bộ chặn bao quanh lời gọi mô hình; bộ nhớ hội thoại lưu lịch sử trò chuyện theo mã hội thoại để mô hình nhớ ngữ cảnh các câu trước.

- Tool Calling: mô hình yêu cầu ứng dụng chạy một phương thức Java (chú thích @Tool), ứng dụng chạy và trả kết quả lại cho mô hình. Điểm an toàn then chốt: mô hình không bao giờ trực tiếp chạm tới cơ sở dữ liệu hay API; mọi thao tác do mã của ứng dụng thực thi và kiểm tra.

- RAG (Retrieval-Augmented Generation): trước khi hỏi mô hình, hệ thống tìm trong kho vector những đoạn dữ liệu liên quan tới câu hỏi rồi ghép vào lời nhắc, nhờ đó câu trả lời dựa trên dữ liệu thật của cửa hàng thay vì kiến thức chung của mô hình. Trong Spring AI việc này do QuestionAnswerAdvisor đảm nhiệm.

Bài giảng dùng Spring AI 2.0.x. Do đồ án chạy trên Spring Boot 3.5 còn nhánh 2.0.x nhắm tới Spring Boot 4, nhóm dùng nhánh 1.1.x (bản 1.1.8) có cùng các khái niệm trên; chi tiết lựa chọn ở mục 5.2.2.

### 5.1.2 Cơ sở dữ liệu vector và embedding

Embedding là cách biến một đoạn văn bản thành một vector số thực sao cho các văn bản gần nghĩa nằm gần nhau trong không gian nhiều chiều. Độ giống giữa hai văn bản được đo bằng độ tương đồng cosine, tức cosine của góc giữa hai vector: gần 1 nghĩa là rất giống, gần 0 là không liên quan. Một Document trong Spring AI gồm mã định danh, nội dung văn bản, siêu dữ liệu (metadata) và vector; kho vector (VectorStore) cho phép thêm tài liệu và tìm kiếm tương đồng với các tham số: số kết quả tối đa (topK), ngưỡng giống nhau tối thiểu (similarityThreshold) và biểu thức lọc theo metadata (ví dụ giá nhỏ hơn một mức). Vector được tính bởi mô hình embedding.

Cơ sở dữ liệu vector tối ưu cho việc tìm tương đồng, nhưng không thay thế SQL cho các phép cộng, nhóm và đếm. Vì vậy hệ thống kết hợp cả hai: SQL Server vẫn là nguồn dữ liệu gốc và nơi thống kê; kho vector chỉ là chỉ mục phụ phục vụ tìm theo ý nghĩa, tìm tour tương tự và làm ngữ cảnh cho trợ lý.

<!-- Bảng 5.1: So sánh tìm kiếm theo từ khoá và tìm kiếm theo ngữ nghĩa -->

| Tiêu chí | Từ khoá (LIKE) | Ngữ nghĩa (vector) |

|---|---|---|

| Cách khớp | Chuỗi ký tự giống nhau | Ý nghĩa gần nhau |

| Ví dụ "biển yên tĩnh cho gia đình" | Không ra kết quả nếu mô tả không chứa đúng cụm này | Ra các tour biển, nghỉ dưỡng phù hợp |

| Chi phí | Rất thấp, chạy ngay trong SQL Server | Cần gọi API embedding (hạn mức, độ trễ) |

| Lọc giá, danh mục | Điều kiện SQL | Biểu thức lọc theo metadata |

| Vai trò trong đồ án | Mặc định và phương án dự phòng | Bật khi có khoá API |

### 5.1.3 Theo dõi hành vi người dùng trên giao diện (UI user tracking)

Theo dõi hành vi là ghi nhận những gì người dùng làm trên giao diện để hiểu họ và cải thiện sản phẩm. Năm chỉ số tiêu biểu: thời gian ở lại trang, số lần bấm vào sản phẩm, số lần thêm vào giỏ, sản phẩm được đặt nhiều nhất và từ khoá tìm kiếm. Các chỉ số này nối thành phễu chuyển đổi (xem, bấm, thêm giỏ, đặt hàng) giúp thấy khách rơi rụng ở bước nào.

Về kỹ thuật thu thập ở trình duyệt: dùng một bộ lắng nghe sự kiện chung cho cả trang (event delegation) để bắt lượt bấm; dùng Page Visibility API để chỉ tính thời gian trang thực sự hiển thị; và gửi sự kiện bằng fetch với tuỳ chọn keepalive để vẫn gửi được khi người dùng đóng tab. Nhóm không dùng sendBeacon vì sendBeacon không đặt được tiêu đề yêu cầu, trong khi hệ thống bật CSRF và cần tiêu đề này.

Về quyền riêng tư và chất lượng dữ liệu: thu thập tối thiểu (dùng mã phiên thay cho thông tin cá nhân), không tin dữ liệu do trình duyệt tự khai (máy chủ tự xác định người dùng và phiên), lọc trình thu thập tự động và giới hạn tốc độ để số liệu không bị nhiễu, có chính sách lưu trữ có thời hạn, và tách việc ghi hành vi khỏi luồng nghiệp vụ để lỗi ghi hành vi không làm hỏng việc đặt tour. Việc thu thập dữ liệu người dùng cần tuân thủ Nghị định 13/2023/NĐ-CP về bảo vệ dữ liệu cá nhân (thông báo và xin đồng ý).

## 5.2 Thiết kế và hiện thực trong TourBooking

### 5.2.1 Kiến trúc tổng thể

Ba nhóm chức năng mới đều đi qua tầng dịch vụ sẵn có của ứng dụng và dùng lại giỏ hàng, truy vấn tour cũ. SQL Server vẫn là nguồn dữ liệu gốc; kho vector là chỉ mục phụ có thể lập lại bất cứ lúc nào từ dữ liệu trong SQL Server.

[[HINH: Kiến trúc các thành phần AI và theo dõi hành vi trong TourBooking]]

<!-- Bảng 5.2: Đối chiếu khái niệm lý thuyết với lớp và tệp trong mã nguồn -->

| Khái niệm (Phần lý thuyết) | Lớp / tệp trong TourBooking |

|---|---|

| ChatClient, advisor, bộ nhớ hội thoại, RAG | AiConfig (bean tourAssistant), AiChatService, ChatApiController |

| Tool Calling | TourTools: searchTours, getDepartures, addToCart |

| Document, metadata, id xác định | TourDocumentFactory |

| VectorStore / SimpleVectorStore | TourVectorStore, TourVectorService (lập chỉ mục, upsert, similaritySearch) |

| Lọc metadata và ngưỡng giống nhau | TourVectorService.search, PriceHintParser |

| Tìm kiếm ngữ nghĩa kèm dự phòng | SemanticSearchService, TourController, TourApiController |

| Tour tương tự, gợi ý cá nhân hoá | RecommendationService |

| Công tắc AI, cấu hình nhà cung cấp | AiProperties, AiEnabledCondition, AiEnvironmentPostProcessor, application.properties |

| Thu thập sự kiện ở trình duyệt | static/js/tracker.js |

| Nhận và kiểm tra sự kiện ở máy chủ | TrackingApiController, TrackingRequest, TrackingService |

| Bảng sự kiện | UserEvent, EventType, EventTypeConverter, UserEventRepository, bảng user_events |

| Thống kê và phễu chuyển đổi | BehaviorStatsService, AdminAnalyticsController, admin/analytics.html |

| Khung chat trên giao diện | fragments/chat-widget.html, static/js/chat.js |

### 5.2.2 Lựa chọn công nghệ và cấu hình

Phiên bản Spring AI: Spring AI 2.0.x dùng Jackson 3 nên nhắm tới Spring Boot 4, trong khi đồ án chạy trên Spring Boot 3.5.16. Nhóm chọn Spring AI 1.1.8, bản 1.x mới nhất tại Maven Central, khai báo qua spring-ai-bom với bốn thành phần: spring-ai-starter-model-google-genai (chat), spring-ai-starter-model-openai (embedding), spring-ai-vector-store và spring-ai-advisors-vector-store. Việc chọn này đã được kiểm chứng bằng biên dịch và chạy thật cùng Spring Boot 3.5.16.

Nhà cung cấp mô hình: dùng Google Gemini vì Gemini có gói miễn phí phù hợp với sinh viên. Phần chat đi qua starter Google GenAI của Spring AI (SDK Gemini chính thức); phần embedding đi qua starter OpenAI trỏ tới điểm truy cập tương thích OpenAI của Gemini. Nhóm tách như vậy vì các mô hình Gemini 3 bắt buộc gửi lại "thought signature" (chữ ký suy luận) của lời gọi hàm trong cùng lượt, mà lớp OpenAI của Spring AI 1.1.8 làm rơi trường này nên chatbot gặp lỗi HTTP 400 ngay khi mô hình gọi công cụ; đường Google GenAI giữ và gửi lại chữ ký. Mô hình chat mặc định là gemini-2.5-flash (có thể đổi sang gemini-2.5-flash-lite nếu cần hạn mức cao hơn) và mô hình embedding mặc định là gemini-embedding-001. Phần chat đổi được sang OpenAI hoặc Ollama chạy cục bộ bằng biến môi trường (thêm AI_CHAT_PROVIDER=openai), không sửa mã; đường này không dùng được cho Gemini 3.

Kho vector: dùng SimpleVectorStore của Spring AI và lưu ra tệp data/vector-store.json (tệp này bị loại khỏi kho mã nguồn). Lý do: môi trường đồ án không có PostgreSQL, và SQL Server không nằm trong danh sách kho vector của bài giảng nên được giữ làm nguồn dữ liệu gốc. Ứng dụng chỉ gọi API embedding khi tệp chưa có hoặc khi đổi mô hình; những lần khởi động sau nạp thẳng từ tệp để tiết kiệm hạn mức.

<!-- Bảng 5.3: Các biến môi trường cấu hình tính năng AI -->

| Biến môi trường | Ý nghĩa | Mặc định |

|---|---|---|

| GEMINI_API_KEY | Khoá API Gemini; thiếu hoặc rỗng thì AI tắt | (trống) |

| AI_ENABLED | Công tắc tường minh; để trống = tự bật khi có khoá | (trống) |

| AI_BASE_URL | Địa chỉ nhà cung cấp | https://generativelanguage.googleapis.com |

| AI_CHAT_PATH / AI_EMBEDDINGS_PATH | Đường dẫn gọi chat (chỉ khi AI_CHAT_PROVIDER=openai) và embedding | /v1beta/openai/chat/completions và /v1beta/openai/embeddings |

| AI_CHAT_MODEL | Mô hình chat | gemini-2.5-flash |

| AI_EMBEDDING_MODEL | Mô hình embedding | gemini-embedding-001 (3072 chiều) |

| AI_EMBEDDING_DIMENSIONS | Số chiều vector yêu cầu; để trống dùng mặc định của mô hình | (trống) |

| AI_SIMILARITY_THRESHOLD | Ngưỡng giống nhau tối thiểu | 0,5 |

| AI_EMBEDDING_MIN_INTERVAL_MS | Khoảng cách tối thiểu giữa hai lần gọi embedding | 700 |

| AI_VECTOR_STORE_PATH | Đường dẫn tệp kho vector | data/vector-store.json |

Một đoạn cấu hình tiêu biểu trong tệp application.properties (giá trị mặc định: địa chỉ Gemini, mô hình gemini-2.5-flash và gemini-embedding-001):

```
spring.ai.model.chat=${app.ai.chat-provider}
spring.ai.google.genai.api-key=${GEMINI_API_KEY:}
spring.ai.google.genai.chat.options.model=${app.ai.chat-model}
spring.ai.openai.api-key=${GEMINI_API_KEY:}
spring.ai.openai.base-url=${app.ai.base-url}
spring.ai.openai.embedding.embeddings-path=${app.ai.embeddings-path}
spring.ai.openai.embedding.options.model=${app.ai.embedding-model}
```

Công tắc AI: tính năng được bật khi có khoá API hoặc khi đặt AI_ENABLED=true; đặt AI_ENABLED=false thì tắt dù có khoá. Một điểm kỹ thuật quan trọng nhóm gặp phải: cấu hình tự động của Spring AI ném lỗi ngay lúc khởi động nếu khoá rỗng, kể cả khi biến môi trường tồn tại nhưng để trống. Vì vậy một lớp xử lý môi trường riêng tắt luôn cấu hình tự động của cả hai starter (chat và embedding) khi AI tắt, bảo đảm ứng dụng luôn khởi động được.

### 5.2.3 Kho vector và lập chỉ mục

Mỗi tour đang bán được chuyển thành một Document. Nội dung gồm tên tour, danh mục, điểm đến, nơi khởi hành, thời lượng, phương tiện, phần điểm nhấn và một đoạn đầu của mô tả; metadata gồm mã tour, mã danh mục, tên danh mục, điểm đến, giá (đồng) và số ngày. Mã Document được tạo xác định từ mã tour (dạng "tour-5") nên lập chỉ mục lại luôn là cập nhật chứ không sinh bản trùng.

Chỉ mục được nạp khi ứng dụng sẵn sàng (sau bước đổ dữ liệu mẫu) trên một luồng riêng để không làm chậm việc mở web: nếu tệp lưu và tệp ghi chú đi kèm khớp với mô hình hiện tại thì nạp tệp, ngược lại gọi API embedding để lập mới. Quản trị viên có thể lập chỉ mục lại bằng nút ở trang "Trợ lý AI" (yêu cầu POST rồi chuyển hướng kèm thông báo). Khi quản trị viên thêm, sửa, xoá hoặc đổi trạng thái một tour, chỉ mục được cập nhật tự động sau khi giao dịch hoàn tất; tour ngừng bán hoặc bị xoá bị gỡ khỏi chỉ mục. Vì SimpleVectorStore gọi API embedding từng tài liệu một, hệ thống giãn cách tối thiểu 700 ms giữa hai lần gọi và cho phép thử lại khi nhà cung cấp trả mã 429 (vượt hạn mức) để phù hợp với gói miễn phí.

### 5.2.4 Tìm kiếm ngữ nghĩa

Ở trang danh sách tour có thêm công tắc "Tìm bằng AI". Khi bật, người dùng gõ một mô tả tự nhiên như "biển yên tĩnh cho gia đình dưới 5 triệu": hệ thống tách cụm chỉ giá ("dưới 5 triệu", "từ 2 đến 4 triệu", "tối đa 3tr") thành bộ lọc giá, rồi tìm tương đồng trong kho vector với số kết quả tối đa, ngưỡng giống nhau và bộ lọc metadata theo giá hoặc danh mục. Các tour trả về được xếp theo độ phù hợp và phân trang như danh sách thường. Cùng chức năng này còn có đường dẫn REST GET /api/tours/semantic.

Phương án dự phòng: nếu AI chưa được cấu hình, nhà cung cấp lỗi, hoặc không có kết quả nào đủ giống, hệ thống lùi về tìm theo từ khoá sẵn có (loại cụm chỉ giá khỏi câu và đổi thành bộ lọc giá), đồng thời hiện thông báo thân thiện. Công tắc bị khoá và có chú thích "AI chưa được cấu hình" khi không có khoá API.

### 5.2.5 Tour tương tự và gợi ý "Có thể bạn thích"

Ở trang chi tiết tour, khối "Tour tương tự" lấy mô tả của chính tour đang xem làm truy vấn, loại bỏ chính nó và hiển thị các tour giống nhất; kết quả được nhớ trong bộ nhớ đến lần lập chỉ mục sau để không gọi API lặp lại. Khi AI tắt, khối này lấy các tour cùng danh mục được xem nhiều nhất.

Ở trang chủ, khối "Có thể bạn thích" gợi ý cho từng tài khoản hoặc từng phiên. Hệ thống đọc các sự kiện trong 30 ngày gần nhất với trọng số xem 1, bấm 2, thêm giỏ 3, lấy tối đa ba tour có điểm cao nhất làm hạt giống, tìm các tour giống chúng bằng vector (loại những tour đã xem) rồi xếp hạng lại theo công thức 0,7 x độ giống + 0,3 x độ phổ biến. Khi AI tắt thì dùng các tour cùng danh mục với tour quan tâm nhất; khi chưa có lịch sử thì hiển thị các tour phổ biến nhất.

### 5.2.6 Trợ lý tour (chatbot)

Khung chat nổi ở góc phải dưới của mọi trang công khai cho phép khách hỏi bằng tiếng Việt. Phía máy chủ, một ChatClient được dựng một lần với lời nhắc hệ thống tiếng Việt, bộ nhớ hội thoại theo từng phiên (mã hội thoại là mã phiên đã băm), QuestionAnswerAdvisor để lấy ngữ cảnh từ kho vector (RAG) và ba công cụ:

- searchTours: tìm tour theo nhu cầu và giá tối đa (dùng lại đúng đường tìm kiếm ngữ nghĩa kèm dự phòng).

- getDepartures: lấy các lịch khởi hành còn chỗ, kèm ngày đi, số chỗ còn và giá.

- addToCart: thêm một lịch khởi hành vào giỏ hàng của phiên, chỉ sau khi khách đồng ý rõ ràng; công cụ gọi lại CartService sẵn có nên mọi kiểm tra (còn bán, đủ chỗ, tối đa 30 khách) vẫn do mã Java đảm nhiệm.

Chatbot không có công cụ nào để thanh toán hay chốt đơn: lời nhắc hệ thống yêu cầu hướng khách vào Giỏ hàng và Thanh toán (cần đăng nhập), và kiểm thử tự động xác nhận lớp công cụ chỉ có đúng ba phương thức trên. Danh tính (phiên, mã tài khoản) được truyền qua ngữ cảnh công cụ ở phía máy chủ chứ không đưa cho mô hình. Mỗi phiên bị giới hạn 8 tin nhắn mỗi phút và mỗi tin tối đa 500 ký tự; nếu nhà cung cấp từ chối vòng gọi công cụ bằng lỗi 4xx (ví dụ báo thiếu thought signature) thì hệ thống thử lại đúng một lần không có công cụ (chỉ dùng RAG, nhắc khách vào trang danh sách tour) và ghi nhật ký mức cảnh báo; khi AI tắt hoặc nhà cung cấp vẫn lỗi, API trả mã 503 kèm câu tiếng Việt thân thiện và khung chat hiển thị trạng thái "AI chưa được cấu hình".

### 5.2.7 Theo dõi hành vi người dùng

Bảng user_events (thực thể UserEvent, mô tả ở mục 3.2.19) ghi mỗi sự kiện một dòng. Bảng cố ý không có khoá ngoại để việc ghi nhanh và không cản việc xoá tour hay tài khoản; mọi truy vấn thống kê nối với bảng tour nên dòng mồ côi không ảnh hưởng. Loại sự kiện là một kiểu liệt kê lưu dạng chuỗi qua bộ chuyển đổi riêng (không dùng ràng buộc kiểm tra của CSDL, đúng quy ước của đồ án).

<!-- Bảng 5.4: Các loại sự kiện và nơi ghi nhận -->

| Loại sự kiện | Khi nào ghi | Nơi ghi nhận |

|---|---|---|

| VIEW | Mở trang chi tiết tour | tracker.js, thuộc tính data-track-view trên trang chi tiết |

| CLICK | Bấm vào ảnh hoặc tên một thẻ tour | tracker.js, thuộc tính data-track-tour trên thẻ tour |

| ADD_TO_CART | Thêm vào giỏ thành công (kể cả qua chatbot) | Phía máy chủ, trong CartApiController, CartController và công cụ addToCart |

| SEARCH | Gửi biểu mẫu tìm kiếm | tracker.js, biểu mẫu có ô tên "q" |

| TIME_ON_PAGE | Rời hoặc ẩn trang chi tiết tour | tracker.js, Page Visibility API |

Quy tắc phía máy chủ khi nhận một sự kiện: chỉ nhận loại sự kiện hợp lệ và đủ trường cần thiết (thiếu thì trả mã 400); mã người dùng lấy từ phiên đăng nhập chứ không tin giá trị trình duyệt gửi; mã phiên được băm SHA-256 trước khi lưu nên không lưu mã phiên đang sống; bỏ qua yêu cầu từ trình thu thập tự động; giới hạn 60 sự kiện mỗi phút cho mỗi phiên (vượt thì trả mã 429); bỏ lượt xem dưới 0,5 giây và cắt ở 30 phút; cắt độ dài từ khoá, chỉ giữ phần đường dẫn của trang (bỏ phần tham số); bỏ sự kiện của tour không tồn tại; lỗi khi ghi CSDL chỉ được ghi nhật ký chứ không làm hỏng nghiệp vụ. Dữ liệu quá 180 ngày được xoá tự động mỗi đêm. Trình duyệt bật chế độ "Do Not Track" sẽ không gửi sự kiện nào.

### 5.2.8 Thống kê hành vi trong khu quản trị

Trang "Hành vi người dùng" trong khu quản trị (cùng phong cách các trang quản trị khác) có bộ lọc khoảng ngày (mặc định 30 ngày, nút nhanh 7/30/90 ngày) và hiển thị: số sự kiện, số phiên truy cập, thời gian xem trung bình; top tour được xem nhiều và được bấm nhiều; phễu chuyển đổi gồm số phiên có xem tour, số phiên có thêm vào giỏ và số đơn đặt (lấy từ các bảng đơn hàng hiện có, không tính đơn đã huỷ) cùng tỉ lệ xem sang giỏ và giỏ sang đơn; phễu chi tiết từng tour (xem, bấm, thêm giỏ, đơn, tỉ lệ bấm/xem, giỏ/xem, đơn/giỏ); tour được đặt nhiều nhất; từ khoá tìm kiếm hàng đầu; và các tour được xem lâu nhất. Mọi số liệu đều dùng truy vấn JPQL, không dùng thủ tục lưu trữ.

[Chèn hình: Hình 5.2 - Màn hình thống kê hành vi người dùng trong khu quản trị (chụp khi chạy với dữ liệu thật)]

[Chèn hình: Hình 5.3 - Khung chat Trợ lý tour và kết quả "Tìm bằng AI" (chụp khi chạy với khoá Gemini thật)]

## 5.3 Hướng dẫn cấu hình và chạy

- Bước 1: Lấy khoá miễn phí tại https://aistudio.google.com/apikey (cần tài khoản Google). Gói miễn phí có giới hạn số yêu cầu mỗi phút và Google có thể dùng nội dung gửi lên để cải thiện sản phẩm, vì vậy không đưa dữ liệu khách hàng thật vào lúc trình diễn.

- Bước 2: Đặt biến môi trường GEMINI_API_KEY. Trong Git Bash: export GEMINI_API_KEY="khoá của bạn". Trong PowerShell: $env:GEMINI_API_KEY="khoá của bạn". Trong IntelliJ: thêm vào mục Environment variables của cấu hình chạy. Không đưa khoá vào mã nguồn.

- Bước 3: Với CSDL đã có sẵn, chạy tệp database/05_user_events.sql để tạo bảng user_events (chạy lại nhiều lần vẫn an toàn). CSDL dựng mới từ 02_schema.sql đã có sẵn bảng này.

- Bước 4: Chạy ứng dụng như thường lệ. Mở trang quản trị "Trợ lý AI" (/admin/ai) để xem trạng thái và số tour đã lập chỉ mục; lần đầu có thể mất vài chục giây vì phải gọi API embedding cho 20 tour.

- Bước 5: Thử "Tìm bằng AI" ở danh sách tour, khung chat ở góc phải, rồi duyệt vài tour và mở trang "Hành vi người dùng" để xem số liệu. Không đặt khoá thì mọi chức năng vẫn chạy, riêng phần AI hiện thông báo chưa được cấu hình.

- Chạy kiểm thử tự động bằng: bash ./mvnw test (không cần SQL Server và không cần khoá).

## 5.4 Kiểm thử và kết quả

Các kiểm thử dưới đây nằm trong thư mục src/test của dự án và chạy bằng một lệnh duy nhất. Kết quả: 86 kiểm thử, tất cả đạt.

<!-- Bảng 5.5: Phạm vi các kiểm thử tự động của chuyên đề AI -->

| Nhóm kiểm thử | Nội dung kiểm chứng | Kết quả |

|---|---|---|

| Đơn vị | Tách cụm chỉ giá; bộ giới hạn tốc độ; TrackingService (lọc bot, băm phiên, hạn mức, cắt thời gian, lỗi CSDL); kiểm tra dữ liệu sự kiện; tính phễu và khoảng ngày; thuật toán gợi ý | Đạt |

| Kho vector | Lập chỉ mục, lưu và nạp tệp không gọi lại API, upsert, gỡ tour cũ, lọc theo giá và danh mục, tour tương tự, đổi mô hình thì lập lại, nhà cung cấp lỗi giữ nguyên chỉ mục cũ | Đạt |

| Chatbot và công cụ | Lời nhắc hệ thống, bộ nhớ theo phiên, giới hạn tốc độ, lỗi nhà cung cấp, thử lại không công cụ khi bị 4xx (khôi phục bộ nhớ, ghi cảnh báo, không thử lại với 401/403/429), vòng gọi công cụ trả lại đúng chữ ký, thử lại 429, công cụ chỉ có 3 phương thức, addToCart đi qua CartService | Đạt |

| Cấu hình | Dựng thật cấu hình Spring AI từ application.properties: không khoá, khoá rỗng, tắt tường minh, có khoá thì chat nối SDK Gemini còn embedding nối điểm truy cập tương thích OpenAI, đổi chat sang Ollama bằng biến môi trường | Đạt |

| Toàn hệ thống (AI bật) | Chạy cả ứng dụng thật trên cơ sở dữ liệu H2 (chế độ SQL Server) với máy chủ giả của Gemini (ép luật thought signature): lập chỉ mục 20 tour, tìm ngữ nghĩa có lọc giá, tour tương tự, theo dõi đủ 5 loại sự kiện và các trường hợp sai, chatbot gọi công cụ rồi thêm vào giỏ, trang thống kê, lập chỉ mục lại, phân quyền | Đạt |

| Toàn hệ thống (AI tắt) | Khoá để trống: trang chủ, tìm kiếm lùi về từ khoá, chatbot trả 503 thân thiện, tour tương tự và gợi ý lùi về cùng danh mục, thống kê tổng hợp đúng trên dữ liệu mẫu | Đạt |

Những gì chưa kiểm chứng được và cần nói rõ: (1) chưa chạy trên SQL Server thật vì môi trường phát triển của chuyên đề này không có SQL Server, nên cần chạy tệp 05_user_events.sql và mở trang thống kê trên máy có SQL Server trước khi trình diễn; (2) chưa gọi Gemini thật, nên độ giống cosine thực tế và ngưỡng 0,5 là ước lượng cần chỉnh khi chạy thật, và việc Gemini 3 trả chữ ký suy luận cho vòng gọi công cụ chỉ mới được kiểm bằng máy chủ giả ép đúng luật đó (chưa xác nhận với dịch vụ thật); (3) giao diện chưa được kiểm bằng mắt trên trình duyệt, nên các hình 5.2 và 5.3 là chỗ chờ chèn ảnh chụp.

## 5.5 Hạn chế

- Bộ nhớ hội thoại của chatbot giữ trong bộ nhớ máy chủ theo từng phiên, chưa có cơ chế giới hạn số phiên hay lưu bền.

- Mã phiên đổi khi khách đăng nhập nên phễu của khách ẩn danh bị tách làm hai phần; số liệu theo tài khoản vẫn liền mạch.

- Chưa có biểu ngữ xin đồng ý theo dõi; hiện mới tôn trọng chế độ Do Not Track và chỉ lưu mã phiên đã băm.

- SimpleVectorStore nạp toàn bộ chỉ mục vào bộ nhớ, đủ cho vài trăm tour; nếu dữ liệu lớn hơn cần chuyển sang kho vector chuyên dụng (ví dụ PostgreSQL với pgvector), không phải sửa các lớp nghiệp vụ nhờ lớp trừu tượng VectorStore.

- Ở chế độ ngữ nghĩa, thứ tự sắp xếp do người dùng chọn bị bỏ qua (kết quả xếp theo độ giống); tìm theo từ khoá dự phòng vẫn khớp nguyên cụm từ.

- Gói miễn phí của Gemini giới hạn số yêu cầu và có thể dùng nội dung để cải thiện sản phẩm của Google; khi triển khai thật cần dùng gói trả phí hoặc chạy mô hình cục bộ.

# CHƯƠNG 6: KẾT LUẬN

## 6.1 Kết quả đạt được

Sau quá trình phân tích, thiết kế và hiện thực, đồ án TourBooking đã đạt được các kết quả cụ thể sau:

Về mặt nghiệp vụ: hệ thống hiện thực đầy đủ **22/22 use case** theo đúng ba nhóm tác nhân đã phân tích, gồm trọn vẹn luồng đặt tour trực tuyến - từ tìm kiếm, giỏ hàng, đăng nhập, áp mã giảm giá, đến xác nhận đơn - lẫn các use case hoàn thành sau cùng (đánh giá tour, quản lý mã khuyến mãi, kiểm duyệt đánh giá, gửi/quản lý liên hệ), tất cả đã được kiểm thử bằng dữ liệu và thao tác thật chứ không chỉ trên lý thuyết. Đáng chú ý nhất: **bốn bảng dữ liệu mở rộng** (đánh giá, liên hệ, thanh toán, lịch sử trạng thái đơn) ban đầu chỉ tồn tại để phục vụ quy tắc chặn xoá, nay **ba trong số đó** đã có chức năng thật ghi/đọc dữ liệu mà **không đổi cấu trúc dữ liệu nào**. Hệ quả đo được bằng thao tác thật: điểm trung bình của tour đổi từ "Chưa có đánh giá" sang **3,0** ngay sau khi duyệt một đánh giá 3 sao; và các quy tắc chặn xoá vốn "chết" trên dữ liệu không phải mẫu có sẵn nay đã "sống dậy" - ví dụ xoá một tour đang có liên hệ hỏi thăm bị chặn, xoá một tài khoản quản trị viên đã từng đổi trạng thái đơn cũng bị chặn - xem Chương 4. Riêng bảng vé đặt lại mật khẩu vẫn là bảng "chết" duy nhất còn lại, có chủ đích vì đồ án chưa xây luồng quên mật khẩu qua email thật.

Về mặt kỹ thuật: dự án xây dựng theo kiến trúc phân tầng rõ ràng, tách bạch giữa tầng giao diện, tầng xử lý nghiệp vụ và tầng dữ liệu; toàn bộ 17 bảng dữ liệu được thiết kế đúng chuẩn hoá, không có bất kỳ ràng buộc, hàm tính toán, thủ tục lưu trữ hay cơ chế tự động kích hoạt nào đặt trong cơ sở dữ liệu (đã kiểm chứng bằng lệnh thật, số lượng bằng 0), mọi quy tắc nghiệp vụ - kể cả các quy tắc khó như chặn xoá dữ liệu theo nhiều tầng tham chiếu và chống bán quá chỗ khi có nhiều người đặt cùng lúc - đều được thực thi minh bạch ngay trong chương trình ứng dụng.

Về trải nghiệm người dùng: giao diện được xây dựng theo một hệ thống thiết kế nhất quán (bảng màu "Ngọc bích & Đất nung", bộ token màu sắc - khoảng cách - bo góc tập trung), hỗ trợ song ngữ Việt/Anh, hiển thị tốt trên nhiều kích thước màn hình, và đã đo đạc thật các chỉ số tương phản màu sắc đạt chuẩn WCAG.

## 6.2 Hạn chế của đồ án

Bên cạnh những kết quả đạt được, đồ án vẫn còn một số hạn chế do giới hạn về thời gian thực hiện:

Thanh toán trực tuyến chưa kết nối cổng thanh toán thật (ngân hàng, ví điện tử); hệ thống hiện chỉ ghi nhận hình thức thanh toán do khách chọn mà chưa xác thực giao dịch thật. Việc gửi email xác nhận đơn cũng đang dùng dịch vụ giả lập ghi ra console thay vì gửi email thật qua SMTP.

Dự án chưa có bộ kiểm thử tự động (unit test/integration test) hoàn chỉnh; các kết quả kiểm thử ở Chương 4 phần lớn dựa trên các lệnh kiểm chứng thủ công đã thực hiện trong quá trình phát triển và được ghi lại làm bằng chứng, một số tình huống cạnh tranh (ví dụ nhiều luồng đặt tour thật sự chạy song song) chưa được đo bằng công cụ kiểm thử tải chuyên dụng.

Quy tắc "không xoá được quản trị viên cuối cùng" vẫn chưa được chứng minh bằng đúng kịch bản thật. Trước đây lý do là dữ liệu mẫu chỉ có một tài khoản quản trị viên nên quy tắc "không tự xoá chính mình" luôn nổ trước; nay đã có tài khoản quản trị viên thứ hai (`admin2`, id=5) nhưng tài khoản này lại vừa đổi trạng thái đơn ở TS23 nên bị chặn bởi quy tắc `error.user.delete.hasBookingHistory` trước khi kịp chạm tới quy tắc "quản trị viên cuối cùng" (xem TS25). Cần một tài khoản quản trị viên thứ ba hoàn toàn "sạch" - chưa từng đổi trạng thái đơn, chưa từng xử lý liên hệ hay duyệt đánh giá - mới cô lập được đúng tình huống này.

## 6.3 Hướng phát triển

Trong thời gian tới, nhóm dự kiến tiếp tục hoàn thiện đồ án theo các hướng sau:

Hoàn thiện giao diện cho hai use case còn thiếu (đánh giá tour, quản lý mã khuyến mãi phía quản trị) để khai thác trọn vẹn tầng dữ liệu và nghiệp vụ đã xây dựng sẵn.

Bổ sung bộ kiểm thử tự động (kiểm tra các truy vấn đếm phục vụ quy tắc chặn xoá, kiểm tra phân quyền, kiểm tra luồng đặt tour đầu cuối) để việc kiểm thử không còn phụ thuộc hoàn toàn vào thao tác thủ công.

Kết nối cổng thanh toán trực tuyến thật (ví điện tử, cổng thanh toán ngân hàng) và dịch vụ gửi email thật, thay cho các thành phần giả lập hiện tại - kiến trúc hiện tại đã tách riêng phần này nên việc thay thế chỉ cần đổi cấu hình mà không ảnh hưởng tới phần còn lại của hệ thống.

Bổ sung chức năng gợi ý tour theo lịch sử đặt/đánh giá của khách hàng, và mở rộng báo cáo thống kê (theo danh mục, theo điểm đến, tỉ lệ huỷ đơn) để hỗ trợ quản trị viên ra quyết định kinh doanh tốt hơn.

# DANH MỤC TÀI LIỆU THAM KHẢO

## Tiếng Việt

[1] Bộ Giáo dục và Đào tạo. Tài liệu học phần Lập trình WWW, Khoa Công nghệ Thông tin, Trường Đại học Công nghiệp Thành phố Hồ Chí Minh.

## Tiếng Anh

[2] Spring Team. (n.d.). *Spring Boot Reference Documentation* (version 3.5.x). VMware. https://docs.spring.io/spring-boot/docs/current/reference/html/

[3] Spring Team. (n.d.). *Spring Data JPA Reference Documentation*. VMware. https://docs.spring.io/spring-data/jpa/reference/

[4] Spring Team. (n.d.). *Spring Security Reference*. VMware. https://docs.spring.io/spring-security/reference/

[5] Thymeleaf Team. (n.d.). *Thymeleaf: Tutorial - Using Thymeleaf*. https://www.thymeleaf.org/documentation.html

[6] Microsoft. (n.d.). *Microsoft JDBC Driver for SQL Server documentation*. https://learn.microsoft.com/en-us/sql/connect/jdbc/

[7] Object Management Group. (2017). *OMG Unified Modeling Language (UML), Version 2.5.1*. https://www.omg.org/spec/UML/

# PHỤ LỤC

## Hướng dẫn sử dụng

### 1. Đăng nhập hệ thống

Mở trình duyệt, truy cập địa chỉ website (mặc định `http://localhost:8081`). Bấm "Đăng nhập" ở góc trên bên phải. Nhập email và mật khẩu, bấm "Đăng nhập". Tài khoản mẫu để kiểm thử (mật khẩu đều `123456`): `admin@tourbooking.vn` (quản trị viên), `an.nguyen@gmail.com`, `binh.tran@gmail.com`, `cuong.le@gmail.com` (khách hàng).

Ghi chú: nếu chưa có tài khoản, bấm "Đăng ký" để tạo tài khoản khách hàng mới.

### 2. Hướng dẫn đặt tour trực tuyến

2.1. Tổng quan: chức năng đặt tour cho phép tìm tour, thêm vào giỏ hàng, áp mã giảm giá và hoàn tất đặt chỉ trong vài bước.

2.2. Quy trình thực hiện:

Bước 1: Vào menu "Tour" hoặc dùng ô tìm kiếm ở trang chủ để tìm tour phù hợp (có thể gõ không dấu).

Bước 2: Bấm vào một tour để xem chi tiết, chọn đợt khởi hành phù hợp, nhập số người lớn/trẻ em, bấm "Thêm vào giỏ".

Bước 3: Mở giỏ hàng (biểu tượng giỏ hàng ở thanh điều hướng) để kiểm tra lại các dòng đã chọn, có thể sửa số lượng hoặc xoá bớt.

Bước 4: Bấm "Tiến hành đặt tour". Nếu chưa đăng nhập, hệ thống sẽ yêu cầu đăng nhập hoặc đăng ký trước; giỏ hàng vẫn được giữ nguyên.

Bước 5: Tại trang thanh toán, kiểm tra lại thông tin liên hệ, có thể nhập mã giảm giá và bấm "Áp dụng" để xem số tiền được giảm.

Bước 6: Chọn hình thức thanh toán, bấm "Xác nhận đặt tour".

Bước 7: Hệ thống hiển thị trang xác nhận kèm mã đơn. Có thể xem lại đơn này bất kỳ lúc nào ở menu "Tài khoản" → "Lịch sử đặt tour".

2.3. Lưu ý quan trọng: số chỗ hiển thị là số chỗ còn trống tại thời điểm xem trang, có thể thay đổi nếu người khác đặt trước; hệ thống sẽ báo lỗi rõ ràng nếu đợt khởi hành hết chỗ ngay trước khi xác nhận.

### 3. Quản lý danh mục, tour và đợt khởi hành (dành cho quản trị viên)

Đăng nhập bằng tài khoản quản trị viên, vào khu vực quản trị (menu riêng chỉ hiện với tài khoản ADMIN).

Vào "Quản lý danh mục" để thêm/sửa danh mục trước, sau đó vào "Quản lý tour" để thêm tour thuộc danh mục đó, kèm ảnh minh hoạ và lịch trình.

Từ trang chi tiết một tour, vào "Quản lý đợt khởi hành" để thêm các đợt khởi hành cụ thể (ngày đi, ngày về, số chỗ, giá).

Khi cần ngừng bán, ưu tiên dùng nút "Ngừng hoạt động"/"Ngừng bán" thay vì "Xoá" nếu đối tượng đã từng phát sinh giao dịch - hệ thống sẽ tự động từ chối và giải thích nếu cố xoá một đối tượng đang được dữ liệu khác tham chiếu.

### 4. Quản lý đơn đặt tour (dành cho quản trị viên)

Vào "Quản lý đơn đặt tour" để xem danh sách, lọc theo trạng thái. Mở một đơn để xem chi tiết, đổi trạng thái theo đúng vòng đời (chờ xác nhận → đã xác nhận → hoàn thành, hoặc huỷ ở bất kỳ bước nào trước khi hoàn thành). Huỷ đơn sẽ tự động hoàn lại số chỗ cho các đợt khởi hành liên quan.

### 5. Đổi ngôn ngữ

Bấm vào nút chọn ngôn ngữ (VI/EN) ở thanh điều hướng để chuyển đổi giữa tiếng Việt và tiếng Anh; trang đang xem và các bộ lọc đang áp dụng sẽ được giữ nguyên.

### 6. Tìm tour bằng AI và dùng Trợ lý tour

Ở trang "Tất cả tour", bật công tắc "Tìm bằng AI" rồi gõ mô tả bằng lời, ví dụ "biển yên tĩnh cho gia đình dưới 5 triệu", bấm "Áp dụng". Kết quả được xếp theo độ phù hợp. Bấm biểu tượng khung chat ở góc phải dưới để hỏi Trợ lý tour về tour, giá và chỗ còn trống; trợ lý có thể thêm lịch khởi hành vào giỏ khi bạn đồng ý, nhưng việc thanh toán luôn do bạn tự thực hiện ở trang Giỏ hàng. Nếu thấy thông báo "AI chưa được cấu hình", quản trị viên chưa đặt khoá API; bạn vẫn dùng ô tìm kiếm thường bình thường.

### 7. Xem thống kê hành vi người dùng (dành cho quản trị viên)

Đăng nhập quản trị viên, chọn "Hành vi người dùng" ở menu bên trái. Chọn khoảng ngày (hoặc bấm 7, 30, 90 ngày) để xem top tour được xem và bấm, phễu chuyển đổi, từ khoá tìm kiếm và thời gian xem trung bình. Chọn "Trợ lý AI" để xem trạng thái AI, số tour đã lập chỉ mục và bấm "Lập chỉ mục lại" sau khi nhập hàng loạt tour mới.

<!-- Mục 3.2.19 (UserEvent) cần chèn thủ công sau 3.2.18; khối nguồn: -->
<!--
### 3.2.19 Thực thể UserEvent

Bảng CSDL tương ứng: user_events

>>ENTITY
| 1 | Khai báo thuộc tính |  |  |  |
| 1.1 | id | Long | Khoá chính, tự tăng |  |
| 1.2 | type | EventType | Bắt buộc | VIEW, CLICK, ADD_TO_CART, SEARCH, TIME_ON_PAGE; lưu dạng chuỗi qua bộ chuyển đổi |
| 1.3 | sessionId | String | Bắt buộc, tối đa 64 ký tự | Mã phiên đã băm SHA-256 |
| 1.4 | userId | Long | Có thể để trống | Lấy từ phiên đăng nhập; không có khoá ngoại |
| 1.5 | tourId | Long | Có thể để trống | Không có khoá ngoại để ghi nhanh |
| 1.6 | departureId | Long | Có thể để trống |  |
| 1.7 | keyword | String | Có thể để trống, tối đa 200 ký tự | Chỉ dùng cho sự kiện tìm kiếm |
| 1.8 | durationMs | Integer | Có thể để trống | Thời gian xem (mili giây), tối đa 30 phút |
| 1.9 | pageUrl | String | Có thể để trống, tối đa 255 ký tự | Chỉ phần đường dẫn, bỏ tham số |
| 1.10 | occurredAt | LocalDateTime | Bắt buộc | Thời điểm xảy ra sự kiện |
| 2 | Phương thức |  |  |  |
| 2.1 | Getter/Setter | - | Theo ràng buộc trên |  |
>>END

-->
