import os
import pandas as pd
import numpy as np
from PIL import Image
from torchvision.transforms import ToTensor
from torchvision import transforms
from torchvision import datasets
import matplotlib.pyplot as plt
from torch.utils.data import DataLoader, random_split
import model_cnn as modelCnn
import torch
import train
import test
import demo
from torch.utils.mobile_optimizer import optimize_for_mobile
# import argparse


raw_yale_path = './data/yalefaces'
preprocessed_data_path = './data/preprocessedYale'
model_saved_path = './savedModel/savedCNN.pt'
label_list = list()

if not os.path.isdir(preprocessed_data_path):
    os.mkdir(preprocessed_data_path)


def main():
    batch = 16
    lr = 0.001
    epoch = 100

    # 데이터셋 -> train,test로 나누기
    # torchvision 라이브러리 사용
    trans = transforms.Compose([transforms.Resize((200, 200)),
                                transforms.ToTensor(),
                                transforms.Grayscale(num_output_channels=1)])

    dataset = datasets.ImageFolder(
        root=preprocessed_data_path, transform=trans)
    train_dataset, test_dataset = random_split(
        dataset, [130, 34])
    train_loader = DataLoader(train_dataset,
                              batch_size=batch, shuffle=True)
    test_loader = DataLoader(test_dataset, batch_size=batch)

    while(1):
        mode = input('train/ test/ demo : ')
        if(mode == 'train'):

            # 모델 생성, 세팅
            device = torch.device(
                'cuda:0' if torch.cuda.is_available() else 'cpu')
            print('running on ' + str(device))
            model = modelCnn.CNN().to(device)
            criterion = torch.nn.CrossEntropyLoss()
            optimizer = torch.optim.SGD(
                model.parameters(), lr=lr, momentum=0.9)
            # 훈련 (훈련 후)
            model = train.train(model, criterion, optimizer,
                                train_loader, device, batch, epoch, lr)

            # 모델 저장
            if not os.path.isdir('./savedModel'):
                os.mkdir('./savedModel')
            # for Android
            model.eval()
            example = torch.rand(1, 1, 200, 200)
            traced_script_module = torch.jit.trace(model, example)
            traced_script_module_optimized = optimize_for_mobile(
                traced_script_module)
            traced_script_module_optimized._save_for_lite_interpreter(
                './savedModel/androidModel.ptl')
            # for python
            torch.save(model, model_saved_path)

            # scripted_model = torch.jit.script(model)
            # opt_model = torch.utils.optimize_for_mobile(scripted_model)
            # torch.jit.save(opt_model, './savedModel/androidModel.pt')

        elif(mode == 'test'):
            # 테스트
            print('test result: ' + str(test.test(test_loader, model_saved_path)))
        elif(mode == 'demo'):
            while(1):
                demo.demo()
        else:
            print('wrong input..')


def devideBySubject():
    # subject별로 ./data/preprocessedYale 폴더에 폴더별로 나눔
    dataNum = 0
    dataSize = None
    for name in os.listdir(raw_yale_path):
        if not 'gif' in name and not 'txt' in name and not 'DS_Store' in name:
            image = Image.open(raw_yale_path + '/' + name)
            subject_name = name.split('.')[0]
            label_list.append(subject_name)
            img_path = preprocessed_data_path + '/' + subject_name + '/' + name + '.jpg'

            if not os.path.isdir(preprocessed_data_path + '/' + subject_name):
                os.mkdir(preprocessed_data_path + '/' + subject_name)
            if not os.path.isfile(img_path):
                image.save(img_path)

            dataNum = dataNum+1
            if dataNum == 1:
                dataSize = Image.open(img_path).size
    print("데이터의 개수: " + str(dataNum))  # 164
    print("데이터의 사이즈: " + str(dataSize))  # 320x243


if __name__ == '__main__':
    main()
