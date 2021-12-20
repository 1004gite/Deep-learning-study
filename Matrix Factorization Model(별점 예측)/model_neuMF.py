import torch
from torch import nn
import torch.nn.functional as F


class Model_neuMF(nn.Module):
    def __init__(self, num_users=610, num_items=193609, rank=4):
        super().__init__()
        self.num_users = num_users
        self.num_items = num_items
        self.rank = rank

        self.embed_U = nn.Embedding(num_users + 1, rank)
        self.embed_V = nn.Embedding(num_items + 1, rank)

        self.fc1 = nn.Linear(rank * 2, 50)
        self.fc2 = nn.Linear(50, 20)
        self.fc3 = nn.Linear(20, 1)

        # self.dropout1 = nn.Dropout(0.2)
        # self.dropout2 = nn.Dropout(0.5)

    def forward(self, users, items):
        embed_user = self.embed_U(users)
        embed_item = self.embed_V(items)

        result = torch.ones(embed_user.shape[0])
        for i in range(embed_user.shape[0]):
            x = torch.cat([embed_user[i].view(-1), embed_item[i].view(-1)], dim=0)
            x = F.relu(self.fc1(x))
            # x = self.dropout1(x)
            x = F.relu(self.fc2(x))
            # x = self.dropout2(x)
            x = self.fc3(x)
            result[i] = x

        return result
