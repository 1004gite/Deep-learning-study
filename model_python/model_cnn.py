import torch.nn as nn
import torch.nn.functional as F


# torch.nn.Conv2d(
#     in_channels,
#     out_channels,
#     kernel_size,
#     stride=1,
#     padding=0,
#     dilation=1,
#     groups=1,
#     bias=True,
#     padding_mode='zeros'
# )

class CNN(nn.Module):

    # 사용하고자 하는 연산 정의

    def __init__(self):

        super(CNN, self).__init__()

        self.conv1 = nn.Conv2d(in_channels=1, out_channels=6,
                               kernel_size=5, padding=2)

        self.conv2 = nn.Conv2d(
            in_channels=6, out_channels=16, kernel_size=5, padding=2)

        self.conv3 = nn.Conv2d(
            in_channels=16, out_channels=32, kernel_size=5, padding=2)

        self.pool = nn.MaxPool2d(kernel_size=2, stride=2)

        self.fc1 = nn.Linear(25*25*32, 120)
        self.fc2 = nn.Linear(120, 84)
        self.fc3 = nn.Linear(84, 15)

    # 연산 순서 정의

    def forward(self, x):
        # 200x200, 3

        x = self.pool(F.relu(self.conv1(x)))  # 100x100, 6

        x = self.pool(F.relu(self.conv2(x)))  # 50x50, 16

        x = self.pool(F.relu(self.conv3(x)))  # 25x25, 32

        x = x.view(-1, 25*25*32)

        x = F.relu(self.fc1(x))

        x = F.relu(self.fc2(x))

        x = self.fc3(x)

        x = F.log_softmax(x)

        return x
