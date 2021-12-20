import argparse

import torch
from torch.utils.data import DataLoader
import torch.nn as nn

from model import ModelClass
from model_neuMF import Model_neuMF
from utils import RecommendationDataset


def inference(data_loader, model):
    """model inference"""

    model.eval()
    loss = 0.0
    count = 0.0
    criterion = nn.MSELoss()

    with torch.no_grad():
        for users, items, ratings in data_loader:
            predicted = model(users, items)

            count += len(ratings)
            loss += torch.sqrt(criterion(predicted, ratings)).item() * len(ratings)

    return loss / count


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="2021 AI Final Project")
    parser.add_argument("--load-model", default="model.pt", help="Model's state_dict")
    parser.add_argument("--dataset", default="./data", help="dataset directory")
    parser.add_argument("--batch-size", default=16, help="test loader batch size")

    args = parser.parse_args()

    # instantiate model
    # model = ModelClass()
    # model.load_state_dict(torch.load(args.load_model))
    model = Model_neuMF()
    model.load_state_dict(torch.load("model_neumf_new8.pt"))

    # load dataset in test folder
    test_data = RecommendationDataset(f"{args.dataset}/mTest.csv", train=True)
    test_loader = DataLoader(test_data, batch_size=args.batch_size, shuffle=False)

    # write model inference
    cost = inference(test_loader, model)

    print("The cost is ", cost)
