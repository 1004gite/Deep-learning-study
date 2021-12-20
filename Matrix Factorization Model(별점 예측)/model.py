import torch
from torch import nn


class ModelClass(nn.Module):
    def __init__(self, num_users=610, num_items=193609, rank=10):
        super().__init__()
        self.embed_U = nn.Embedding(num_users + 1, rank)
        self.embed_V = nn.Embedding(num_items + 1, rank)

        self.predict_layer = torch.ones(rank, 1)

    def forward(self, users, items):
        embed_user = self.embed_U(users)
        embed_item = self.embed_V(items)

        output_GMF = embed_user * embed_item
        prediction = torch.matmul(output_GMF, self.predict_layer)
        return prediction.view(-1)
