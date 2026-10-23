# NextBill — Google Stitch 프롬프트

- 기준 문서: `docs/superpowers/specs/2026-10-23-nextbill-design.md` §9 화면 목록
- 사용법:
  1. Stitch에서 **Mobile**을 선택하고 아래 "0. 공통 프롬프트"로 첫 화면(홈)을 만듭니다.
  2. 같은 프로젝트에서 1~7번 프롬프트를 한 화면씩 이어서 넣습니다. 스타일이 유지되도록 같은 프로젝트를 씁니다.
  3. 결과물(스크린샷, `code.html`, 테마 정보)을 `docs/design/`에 넣어 주시면 `core:designsystem` 토큰으로 옮깁니다.
- 프롬프트는 영어로 썼습니다. Stitch의 해석이 영어에서 더 안정적이기 때문입니다. 앱 화면 문구는 한국어로 지정했습니다.
- 스타일 방향(토스 같은 친숙한 한국 앱 느낌)은 Beenway 때 선호하신 방향을 참고한 **제 추측**입니다. 다르면 0번의 Style 부분만 바꾸면 됩니다.

---

## 0. 공통 프롬프트 (첫 화면과 함께 입력)

```
Design an Android mobile app called "NextBill", a personal subscription and recurring-payment tracker. Users register recurring payments (Netflix, phone bill, insurance, cloud storage), see when the next payment is due, and see how much will be charged this month. All data stays on the device; there is no login.

Platform: Android, Material 3 (Material You). Use Material 3 components, a top app bar, and a floating action button. Respect Android conventions, not iOS.

Style: clean, familiar Korean fintech app feel (similar in spirit to Toss). White and light-gray surfaces, generous spacing, soft rounded cards (16dp corners), one sans-serif typeface (Pretendard or a similar neutral Korean sans-serif), large bold numbers for money, and calm secondary text. One primary accent color: a trustworthy deep blue. Use a separate warning color only for payments due today or tomorrow. Do not use gradients, illustrations of people, or stock photos. Use Material Symbols icons.

Each subscription is shown with a circular avatar: the first letter of its name on a colored background (no brand logos).

Language: all UI text in Korean. Currency format: "₩17,000", plus "$9.99" for foreign currencies.

First screen — Home:
- Top app bar: title "NextBill", settings icon on the right.
- Summary card at top:
  - "이번 달 결제 예정" with a big amount "₩86,400"
  - Below it, smaller: "월 고정비 ₩72,300"
  - If there are USD subscriptions, a second line "+ $19.98"
- Section title "다가오는 결제"
- List of 6 items, sorted by date. Each row shows: avatar, name, cycle text (e.g. "매월 15일", "매년 3월 2일", "2주마다"), amount on the right, and a D-day chip ("오늘", "내일", "D-5").
  - Example items: 넷플릭스 ₩17,000 오늘; 통신비 ₩55,000 내일; 유튜브 프리미엄 ₩14,900 D-5; iCloud+ $2.99 D-9; 자동차보험 ₩480,000 매년 D-41; 헬스장 ₩60,000 매월 D-12
- Floating action button "+" labeled "구독 추가"
```

## 1. 홈 — 빈 상태

```
Create the empty state of the Home screen, keeping the same style. There are no subscriptions yet. Show a simple Material Symbols icon (receipt or calendar), the title "등록된 구독이 없어요", the description "반복 결제를 등록하면 다음 결제일을 알려드려요", and a primary button "첫 구독 추가하기". Keep the top app bar. The summary card shows ₩0 in a muted style.
```

## 2. 구독 등록·수정

```
Create the "구독 추가" (Add subscription) form screen in the same style. Top app bar with a back arrow, the title "구독 추가", and a "저장" text button on the right.

Fields, top to bottom, grouped in cards:
1. Avatar preview (first letter + selected color) and a "이름" text field (placeholder "예: 넷플릭스")
2. "금액" with a large numeric field and a currency selector chip ("KRW ▾")
3. "결제 주기": a number stepper ("1") plus segmented buttons "일 / 주 / 개월 / 년", with helper text below: "매월 15일에 결제돼요"
4. "첫 결제일": a date field that opens a Material date picker
5. "카테고리": a horizontal chip group (영상, 음악, 통신, 보험, 클라우드, 생활, 기타)
6. "색상": a row of 8 circular color swatches
7. "결제 알림": a switch; when on, show "며칠 전" chips (당일, 1일 전, 3일 전, 7일 전) and "알림 시각 오전 9:00"
8. "메모": a multiline text field

Show one validation error example: the amount field with the red helper text "금액을 입력해 주세요".
```

## 3. 구독 상세

```
Create the subscription detail screen for "넷플릭스" in the same style. Top app bar with a back arrow and an edit (pencil) icon.

Content:
- Large avatar, the name "넷플릭스", the category chip "영상"
- Big amount "₩17,000" with "매월 15일" below
- Info card: "월 환산 ₩17,000", "연 환산 ₩204,000", "결제 알림 1일 전 오전 9:00"
- "다가오는 결제일" card with 3 rows: "11월 15일 (토)", "12월 15일 (월)", "1월 15일 (목)"
- Memo card
- At the bottom: an outlined button "해지 처리" and a text button "삭제" in the error color
```

## 4. 해지한 구독

```
Create the "해지한 구독" (Cancelled subscriptions) list screen in the same style. Top app bar with a back arrow. List rows show the avatar (desaturated), the name, the former amount and cycle, and "3월 2일 해지". Each row has an overflow menu with "다시 사용" (restore) and "삭제". Include a short info text at the top: "해지한 구독은 합계와 알림에서 제외돼요".
```

## 5. 설정

```
Create the Settings screen in the same style, using Material 3 list items grouped under section headers:
- "알림": 기본 알림 시점 (1일 전), 기본 알림 시각 (오전 9:00), 알림 권한 상태 ("허용됨")
- "화면": 테마 (시스템 설정 / 라이트 / 다크), 동적 색상 사용 (switch)
- "기본값": 기본 통화 (KRW)
- "데이터": 백업 파일 내보내기, 백업 파일 가져오기 (subtitle: "현재 데이터가 모두 바뀌어요"), 해지한 구독 보기
- "정보": 오픈소스 라이선스, 앱 버전 1.0.0
At the bottom, small muted text: "모든 데이터는 이 기기에만 저장돼요".
```

## 6. 홈 화면 위젯 (4×2)

```
Design an Android home screen widget (4x2 cells) for NextBill, shown on a phone home screen with a plain wallpaper. Material 3 widget style with large rounded corners and a surface background.
- Header: "NextBill" small, and on the right "이번 달 ₩86,400"
- 3 compact rows: avatar, name, amount, D-day chip (넷플릭스 오늘, 통신비 내일, 유튜브 프리미엄 D-5)
Also show a smaller 2x2 variant: "다음 결제" label, name "넷플릭스", "₩17,000", chip "오늘".
```

## 7. 알림 권한 안내 시트

```
Create a Material 3 modal bottom sheet on top of the Add subscription screen. Notification icon at the top, the title "결제 전에 알려드릴게요", the description "결제일 알림을 받으려면 알림 권한이 필요해요. 언제든 설정에서 바꿀 수 있어요.", a primary button "알림 허용하기", and a text button "나중에".
```

## 8. (선택) 다크 모드

```
Show the Home screen from step 0 in dark mode, keeping the same layout. Use Material 3 dark surfaces (not pure black), and keep the money numbers high-contrast.
```

---

# 2차 수정 프롬프트 (2026-10-23, 1차 시안 검토 반영)

> 반영 대상: 설계 문서 §9 내비게이션, §9.1 시안과 다르게 구현하는 점, F11 캘린더.
> Stitch에서는 **수정할 화면을 선택한 뒤** 해당 프롬프트를 넣습니다. A는 프로젝트 전체에 한 번 넣습니다.

## A. 전체 공통 (먼저 한 번)
```
Apply these changes to every screen in this NextBill project:
1. Font: replace Plus Jakarta Sans with "Noto Sans KR" for all text (Korean, Latin and numbers). Keep the existing font sizes, weights, line heights and letter spacing from the design system.
2. Navigation: use a bottom navigation bar with exactly 3 tabs — 홈 / 캘린더 / 설정 — on the three top-level screens (Home, Calendar, Settings) only. Remove the settings gear icon from all top app bars. Detail, Add/Edit, Cancelled subscriptions and the permission sheet are pushed screens: they have a back arrow and no bottom navigation bar.
3. Keep everything else in the design system unchanged (colors, 16dp cards, chips, avatars, spacing).
```

## B. 홈 (00-home, 08-home-dark 각각)
```
On the Home screen: remove the "내역보기 >" link, remove the "다음 달 예상 고정비 변동" card at the bottom, and remove the "날짜순" sort toggle next to "다가오는 결제" (the list is always sorted by date). Keep the "6" count badge. Make sure the "구독 추가" floating action button does not overlap any card; add bottom padding to the list. Change the month chip to "11월".
```

## C. 구독 상세 (03-detail)
```
On the subscription detail screen: remove the "스탠다드 멤버십" line under the name, and remove the small play badge on the avatar. In "다가오는 결제일", change the "D-Day" chip of the first row to "오늘" with the same urgent style used on Home.
```

## D. 구독 등록·수정 (02-editor)
```
On the Add subscription screen: remove the bottom "저장하기" button; keep only the "저장" text button in the top app bar. The avatar preview at the top must use the currently selected color swatch (red is selected, so the avatar background is red with a white letter).
```

## E. 해지한 구독 (04-archive)
```
On the Cancelled subscriptions screen: remove the filter (tune) icon in the top app bar and remove the "정렬: 최근 해지순" label. Remove the bottom navigation bar (this is a pushed screen with a back arrow).
```

## F. 설정 (05-settings)
```
On the Settings screen: remove the back arrow in the top app bar (Settings is a tab now); keep the title "설정" and the bottom navigation bar with 설정 selected.
```

## G. 홈 화면 위젯 (06-widget)
```
On the widget mockup: remove the "지출 리포트" widget entirely. On the 4x2 widget, remove the refresh icon and the "+ 추가" button; keep the header, "이번 달 예정 ₩86,400", the 3 rows, and the footer text "3개 결제 예정". Keep the 2x2 "다음 결제" widget as it is.
```

## H. 캘린더 (신규)
```
Create the "캘린더" (Calendar) tab screen in the same NextBill style. Top app bar: title "캘린더" with previous/next month arrows around "2026년 11월". Show a Material 3 monthly calendar grid where days with payments have small colored dots (the subscription colors, max 3 dots per day). Today is outlined. Below the calendar, a summary row: "이번 달 7건 · ₩86,400". When a day is selected (15일), show a card list under the calendar with that day's payments: avatar, name, amount and cycle text (e.g. 넷플릭스 ₩17,000 매월 15일). Use the bottom navigation bar (홈 / 캘린더 / 설정) with 캘린더 selected. Also create the dark mode version of this screen.
```

## export 요청
수정 후 **전체 화면을 다시 export**합니다(캘린더 라이트·다크 포함). 기존 `docs/design/stitch/` 폴더를 교체합니다.
