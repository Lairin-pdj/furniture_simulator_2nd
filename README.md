# furniture_simulator_2nd
furniture simulator by android studio(google ar core)

## 프로젝트 및 대략적인 기술 설명
android studio을 이용하여 제작된 안드로이드 앱의 소스코드 전반입니다.   
카메라와 google AR core을 이용하여 가구를 직접 설치해볼 수 있는 증강현실 앱입니다.   
사물의 렌더링을 opengl을 이용하여 구현하였습니다.   
obj파일과 mtl파일을 렌더링 할 수 있습니다.   
opencv를 통해 찍은 사진에서 물체를 인식하고 그것을 가구 모델로 제작할 수 있는 기능을 가지고 있습니다.   
    
#### <현재 구현 예정>
aws을 이용한 서버를 통해 가구 데이터를 서로 주고 받을 수 있는 생태를 구현할 예정입니다.   
서버에서는 db(mysql)와 php를 통해 기본적인 https 웹서버를 구현할 것입니다.   
이러한 데이터를 안드로이드 앱에서는 json형식의 데이터로 받아드리고 어댑터를 이용하여 사용자에게 보여주려고 합니다.   
우선적으로 다운로드 기능을 먼저 구현하며, 보안의 문제나 여러가지 문제점에 대한 해결이 되면 업로드 또한 가능하도록 구현할 것입니다.   
   
   
   
## 개발 진행 과정

- 3rd weeks first commit (21-09-19)
1. 가구 삭제 버튼 추가
2. 업로드, 다운로드 버튼 추가
3. 다운로드 액티비티 추가
4. 다운로드 액티비티 UI 일부 구현
     
- 4th week commit (21-09-26)
1. 서버 구축 및 통신 기능 추가
2. 가구 삭제 기능 사이드 이펙트 제거
3. 다운로드 액티비티 어댑터 구현
4. 다운로드 액티비티 UI 추가 및 특정 상황 체킹 추가 구현
      
- 5th week commit (21-10-03)
1. androidx로 전환
2. DB 변경에 따른 세부 클래스 및 통신부 수정
3. 다운로드 리스트 프리뷰 기능 추가
4. 다운로드 리스트 검색 UI 추가
5. 다운로드 리스트 검색 기능 추가
   
- 6th week commit (21-10-10)
1. 가구 삭제 기능에서 프리뷰 파일이 삭제되지 않는 버그 수정
2. 가구 다운로드 기능 구현
3. DB 다운로드 수 기능 구현
   
- 7th week commit (21-10-17)
1. 가구 삭제 버튼 온오프화
2. 삭제하려는 가구가 선택되있을 경우 해제하도록 수정
3. 삭제 기능 편의성 및 UI 개선
4. 업로드 기능 UI 일부 구현
5. 어플리케이션 서브메뉴 UI 일부 구현
