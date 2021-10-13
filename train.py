
def train(model, lossfunc, optimizer, train_loader, device, batch=16, epoch=100, lr=0.001):

    # 모델 훈련
    for epoch in range(1, epoch):  # 10회 반복
        running_loss = 0.0

        for i, data in enumerate(train_loader, 0):

            inputs, labels = data[0].to(device), data[1].to(device)  # 배치 데이터

            optimizer.zero_grad()  # 배치마다 optimizer 초기화

            outputs = model(inputs)  # 노드 10개짜리 예측값 산출
            # 크로스 엔트로피 손실함수 계산    optimizer.zero_grad() # 배치마다 optimizer 초기화
            loss = lossfunc(outputs, labels)
            loss.backward()  # 손실함수 기준 역전파
            optimizer.step()  # 가중치 최적화

            running_loss += loss.item()
        print('[%d] loss: %.3f' %
              (epoch + 1, loss.item()))
    print('final: %.3f' %
          (running_loss / len(train_loader)))

    return model
