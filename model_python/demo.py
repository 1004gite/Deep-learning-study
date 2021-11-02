import torch
from torchvision import transforms
from PIL import Image


def demo():
    num = input('input subjectnum you want to predict(01~15) //ex) 03: ')
    input_path = './data/preprocessedYale/subject' + \
        str(num)+'/subject'+str(num)+'.normal.jpg'

    trans = transforms.Compose([transforms.Resize((200, 200)),
                                transforms.ToTensor(),
                                # transforms.Grayscale(num_output_channels=1)
                                ])
    # find similar
    if num == 'custom':
        input_path = './data/custom.jpg'
        trans = transforms.Compose([transforms.Resize((200, 200)),
                                    transforms.ToTensor(),
                                    transforms.Grayscale(num_output_channels=1)])

    img = Image.open(input_path)
    image = trans(img)

    image = image.unsqueeze(0)
    # print(image.shape)

    model = torch.load('./savedModel/savedCNN.pt')
    model.eval()

    output = model(image)
    result = torch.max(output, 1)

    label_index = int(str(int(result.indices[0])))+1

    print("Predict subject is subject: "+str(label_index) + '\n\n')
