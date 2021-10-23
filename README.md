# furniture_simulator_2nd
furniture simulator by android studio(google ar core)

## 프로젝트 및 대략적인 기술 설명
android studio을 이용하여 제작된 안드로이드 앱의 소스코드 전반입니다.   
카메라와 google AR core을 이용하여 가구를 직접 설치해볼 수 있는 증강현실 앱입니다.   
사물의 렌더링을 opengl을 이용하여 구현하였습니다.   
obj파일과 mtl파일을 렌더링 할 수 있습니다.   
opencv를 통해 찍은 사진에서 물체를 인식하고 그것을 가구 모델로 제작할 수 있는 기능을 가지고 있습니다.   
aws을 이용하여 웹서버를 유지하고 서버를 통해 가구 데이터를 사용자가 서로 주고 받을 수 있는 생태를 구현하였습니다.   
서버는 db(mysql)와 php를 이용하여 데이터를 처리합니다.   
이러한 데이터들을 안드로이드 앱과 json형식으로 교환하며 앱에서는 어댑터를 이용하여 사용자에게 보여지도록 되어있습니다.   
그 외 기본적인 앱이 갖춰야할 데이터 관리, 설정등의 기능들이 구현되어있습니다.   
처음보는 방식이나 api들을 최대한 많이 접하고 사용해보는 것을 목적으로 다양한 기술들을 접목시켰습니다.   
사용자가 불편함을 겪지 않도록 직관성과 유효성을 고려한 UI를 설계하기 위해 노력하였습니다.
</br>  
</br>  
   
## 개발 진행 과정

- 3rd weeks first commit (~ 21-09-19)
1. 가구 삭제 버튼 추가
2. 업로드, 다운로드 버튼 추가
3. 다운로드 액티비티 추가
4. 다운로드 액티비티 UI 일부 구현
</br>   

- 4th week commit (~ 21-09-26)
1. 서버 구축 및 통신 기능 추가
2. 가구 삭제 기능 사이드 이펙트 제거
3. 다운로드 액티비티 어댑터 구현
4. 다운로드 액티비티 UI 추가 및 특정 상황 체킹 추가 구현
</br>  

- 5th week commit (~ 21-10-03)
1. androidx로 전환
2. DB 변경에 따른 세부 클래스 및 통신부 수정
3. 다운로드 리스트 프리뷰 기능 추가
4. 다운로드 리스트 검색 UI 추가
5. 다운로드 리스트 검색 기능 추가
</br>  

- 6th week commit (~ 21-10-10)
1. 가구 삭제 기능에서 프리뷰 파일이 삭제되지 않는 버그 수정
2. 가구 다운로드 기능 구현
3. DB 다운로드 수 기능 구현
</br>  

- 7th week commit (~ 21-10-17)
1. 가구 삭제 버튼 온오프화
2. 삭제하려는 가구가 선택되있을 경우 해제하도록 수정
3. 삭제 기능 편의성 및 UI 개선
4. 업로드 기능 UI 일부 구현
5. 어플리케이션 서브메뉴 UI 일부 구현
</br>  

- 8th week commit (~ 21-10-24)
1. 전체적인 UI 개선
2. 액티비티간 전환 애니메이션 적용
3. 튜토리얼 애니메이션 적용
4. recyclerview 애니메이션 적용
5. 폰트 적용
6. 업로드 기능 구현
7. 서브메뉴 구현
8. 배터리 및 시간 표시 기능 구현
9. splash 화면 구현
10. 설정 액티비티 및 기능 구현
11. 2D, 3D 액티비티를 TabLayout을 이용하여 create 액티비티로 통합
12. 도움말 액티비티 추가 및 viewpager 적용
13. Plane 초기화 기능 구현 및 리셋 기능을 모델과 Plane으로 분리
14. 삭제 및 업로드 모드 관련 접근성 수정
15. 기본가구 관련 업로드, 삭제 제한 및 문구 추가
16. 검색바 기종에 따른 버그 수정
17. 테마관련 버그 수정
</br>  
