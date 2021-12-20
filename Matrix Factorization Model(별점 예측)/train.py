import argparse

import numpy as np
import torch
import torch.nn as nn
from torch.utils.data import DataLoader

from model import ModelClass
from model_neuMF import Model_neuMF
from utils import RecommendationDataset


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="2021 AI Final Project")
    parser.add_argument("--save-model", default="model.pt", help="Model's state_dict")
    parser.add_argument("--dataset", default="./data", help="dataset directory")
    parser.add_argument("--batch-size", default=16, help="train loader batch size")

    args = parser.parse_args()

    # load dataset in train folder
    dataset = RecommendationDataset(f"{args.dataset}/mTrainVaild.csv", train=True)
    n_users, n_items, n_ratings = dataset.get_datasize()

    # train/vaild dataset 분할 및 dataloader 생성
    train_size = int(0.9 * len(dataset))
    vaild_size = len(dataset) - train_size
    train_dataset, vaild_dataset = torch.utils.data.random_split(
        dataset, [train_size, vaild_size]
    )

    # train_loader = DataLoader(dataset, batch_size=16, shuffle=True)
    train_loader = DataLoader(train_dataset, batch_size=16, shuffle=True)
    validation_loader = DataLoader(vaild_dataset, batch_size=16, shuffle=False)

    # instantiate model
    # model = ModelClass()
    model = Model_neuMF()

    optimizer = torch.optim.Adam(model.parameters(), lr=0.005, weight_decay=1e-5)
    criterion = nn.MSELoss()

    # Training part
    for epoch in range(10):
        cost = 0
        for users, items, ratings in train_loader:
            optimizer.zero_grad()
            predict = model(users, items)
            loss = torch.sqrt(criterion(predict, ratings))

            loss.backward()
            optimizer.step()
            cost += loss.item() * len(ratings)

        cost /= train_size
        print(f"Epoch: {epoch}")
        print("train cost: {:.6f}".format(cost))

        cost_test = 0
        for users, items, ratings in validation_loader:
            pred = model(users, items)
            loss = torch.sqrt(criterion(pred, ratings))
            cost_test += loss.item() * len(ratings)

        cost_test /= vaild_size
        print("vaild cost: {:6f}".format(cost_test))

    torch.save(model.state_dict(), args.save_model)
