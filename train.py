import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from PIL import Image
from torch.utils.data import DataLoader, random_split
import torch
from torchvision import datasets
from torchvision.transforms import ToTensor
from torchvision import transforms
import os
import model_cnn as modelCnn

raw_yale_path = './data/yalefaces'
dest_path = './data/preprocessedYale'
label_list = list()

if not os.path.isdir(dest_path):
    os.mkdir(dest_path)


def devideBySubject():
    # subject별로 ./data/preprocessedYale 폴더에 폴더별로 나눔
    dataNum = 0
    dataSize = None
    for name in os.listdir(raw_yale_path):
        if not 'gif' in name and not 'txt' in name and not 'DS_Store' in name:
            image = Image.open(raw_yale_path + '/' + name)
            subject_name = name.split('.')[0]
            label_list.append(subject_name)
            img_path = dest_path + '/' + subject_name + '/' + name + '.jpg'

            if not os.path.isdir(dest_path + '/' + subject_name):
                os.mkdir(dest_path + '/' + subject_name)
            if not os.path.isfile(img_path):
                image.save(img_path)

            dataNum = dataNum+1
            if dataNum == 1:
                dataSize = Image.open(img_path).size
    print("데이터의 개수: " + str(dataNum))  # 164
    print("데이터의 사이즈: " + str(dataSize))  # 320x243


def train():
    # 기본 설정
    batch = 12
    epoch = 50
    lr = 0.002
    device = torch.device('cuda:0' if torch.cuda.is_available() else 'cpu')
    print('running on ' + str(device))

    # 데이터셋 -> train,test로 나누기
    trans = transforms.Compose([transforms.Resize((200, 200)),
                               transforms.ToTensor()])

    dataset = datasets.ImageFolder(root=dest_path, transform=trans)
    # dataset = dataset.reshape((164, 200, 200, 1))  # 개수, 가로, 세로, 차원
    train_dataset, test_dataset = random_split(
        dataset, [130, 34])

    train_loader = DataLoader(train_dataset,
                              batch_size=batch, shuffle=True)
    test_loader = DataLoader(test_dataset, batch_size=batch)
    # for i, (img, label) in enumerate(train_loader):
    #     print(img.shape)

    # 모델 생성, 세팅
    model = modelCnn.CNN().to(device)
    criterion = torch.nn.CrossEntropyLoss()
    optimizer = torch.optim.SGD(model.parameters(), lr=lr, momentum=0.9)

    # 모델 훈련
    for epoch in range(1, epoch):  # 10회 반복
        running_loss = 0.0

        for i, data in enumerate(train_loader, 0):

            inputs, labels = data[0].to(device), data[1].to(device)  # 배치 데이터

            optimizer.zero_grad()  # 배치마다 optimizer 초기화

            outputs = model(inputs)  # 노드 10개짜리 예측값 산출
            # 크로스 엔트로피 손실함수 계산    optimizer.zero_grad() # 배치마다 optimizer 초기화
            loss = criterion(outputs, labels)
            loss.backward()  # 손실함수 기준 역전파
            optimizer.step()  # 가중치 최적화

            running_loss += loss.item()
        print('[%d] loss: %.3f' %
              (epoch + 1, loss.item()))
    print('final: %.3f' %
          (running_loss / len(train_loader)))

    if not os.path.isdir('./savedModel'):
        os.mkdir('./savedModel')
    torch.save(model, './savedModel/savedCNN.pt')


train()
# devideBySubject()
