import torch
import torchvision
import PIL


num = input('input subjectnum you want to predict: ')

img = PIL.Image.open('./data/preprocessedYale/subject' +
                     str(num)+'/subject'+str(num)+'.normal.jpg').convert('RGB')
img.resize((200, 200))
print(img.size)
tf = torchvision.transforms.ToTensor()
img_t = tf(img)
print(img_t.shape)


model = torch.load('./savedModel/savedCNN.pt')
# out = model(img_t)

# print(out)
