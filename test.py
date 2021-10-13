import torch
from torch.autograd import Variable


def test(loader, modelPath):
    model = torch.load(modelPath)
    model.eval()
    validation_loss = 0
    correct = 0
    for images, labels in loader:
        images, labels = Variable(images, volatile=True), Variable(labels)

        output = model(images)
        validation_loss += torch.nn.functional.nll_loss(
            output, labels, size_average=False)
        pred = output.data.max(1, keepdim=True)[1]
        correct += pred.eq(labels.data.view_as(pred)).cpu().sum()

        validation_loss /= len(loader.dataset)
        # print('\n' + ' set: Average loss: {:.4f}, Accuracy: {}/{} ({:.0f}%)\n'
        #       .format(validation_loss, correct, len(loader.dataset),
        #               100. * correct / len(loader.dataset)))

    return 100 * correct / len(loader.dataset)
