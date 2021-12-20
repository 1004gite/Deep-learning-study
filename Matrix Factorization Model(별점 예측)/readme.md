## Neural Collaborative Filtering 논문을 참조하여 모델을 구현

## user, item matrix로부터 특징을 추출하여 NN의 input으로 주어 별점을 예측

### user, item matrix는 각 (# of user * 4), (# of item * 4)의 크기를 가짐

### NN은 각 matrix를 flatten한 후 concate한 입력을 받음

### NN : 2 layer {4*2 -> 4 -> 1}

### 주어진 test 데이터 기준 (RMSE) 기준 0.83정도의 cost를 가짐

### 사용자와 아이템의 특징을 추출한 matrix로부터 다른 사용자, 다른 아이템의 별점 점수를 예상
