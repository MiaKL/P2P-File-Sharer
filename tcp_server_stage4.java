import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class tcp_server_stage4 {
    public static byte[] toBytes (Integer integer) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(integer);
        buffer.rewind();
        return buffer.array();
    }
    public static void errorCloseConnection(Socket socket, String errorMsg) {
        System.out.println("Closed connection due to error: " + errorMsg);
        try {
            socket.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    public static boolean validateClientMessage(Socket socket, String receivedMsg, String expectedMsg) {
        if (receivedMsg == null) receivedMsg = "null";
        receivedMsg = receivedMsg.trim();
        if (receivedMsg.equals(expectedMsg)) {
            System.out.println("Received client message: " + receivedMsg);
            return true;
        } else {
            errorCloseConnection(socket, "Expected \"" + expectedMsg + "\" from client but received \"" + receivedMsg + "\"");
            return false;
        }
    }
    public static void createChunks(String folderPath, String fileName, int numChunks) {
        File file = new File(folderPath + "/" + fileName);

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());

            int bytesPerChunk = fileBytes.length / numChunks;
            if (fileBytes.length - (bytesPerChunk * (numChunks - 1)) > numChunks) {
                ++bytesPerChunk;
            }

            int curChunkStartIndex = 0;
            int curChunkIndex = 0;

            List<byte[]> fileChunks = new ArrayList<byte[]>();

            // break full fileBytes array into chunks and add them to fileChunks
            while (curChunkIndex < (numChunks - 1)) {
                byte[] chunk = Arrays.copyOfRange(fileBytes, curChunkStartIndex, (curChunkStartIndex + bytesPerChunk));
                fileChunks.add(curChunkIndex, chunk);
                curChunkStartIndex += bytesPerChunk;
                ++curChunkIndex;
            }

            // add last chunk (which may have fewer bytes than the others)
            if (curChunkStartIndex < fileBytes.length) {
                byte[] chunk = Arrays.copyOfRange(fileBytes, curChunkStartIndex, fileBytes.length);
                fileChunks.add(curChunkIndex, chunk);
                curChunkStartIndex += bytesPerChunk;
                ++curChunkIndex;
            }

            // get name of file without extension for naming chunks
            int dotIndex = fileName.indexOf('.');
            String fileNameNoExtension = fileName.substring(0, dotIndex);

            // write all chunks to individual files
            for (int i = 0; i < fileChunks.size(); ++i) {
                System.out.println("Created chunk " + i);
                FileOutputStream outToFileLocation = new FileOutputStream(folderPath + "/" + fileNameNoExtension + "_chunk" + i + ".bin");
                outToFileLocation.write(fileChunks.get(i));
                outToFileLocation.flush();
                outToFileLocation.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static int createChunksTenKB(String folderPath, String fileName) {
        File file = new File(folderPath + "/" + fileName);

        int numChunks = 0;

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());

            int bytesPerChunk = 10000;

            int curChunkStartIndex = 0;
            int curChunkIndex = 0;

            numChunks = fileBytes.length / bytesPerChunk;
            if (fileBytes.length % bytesPerChunk != 0) ++numChunks;

            List<byte[]> fileChunks = new ArrayList<byte[]>();

            // break full fileBytes array into chunks and add them to fileChunks
            while (curChunkIndex < (numChunks - 1)) {
                byte[] chunk = Arrays.copyOfRange(fileBytes, curChunkStartIndex, (curChunkStartIndex + bytesPerChunk));
                fileChunks.add(curChunkIndex, chunk);
                curChunkStartIndex += bytesPerChunk;
                ++curChunkIndex;
            }

            // add last chunk (which may have fewer bytes than the others)
            if (curChunkStartIndex < fileBytes.length) {
                byte[] chunk = Arrays.copyOfRange(fileBytes, curChunkStartIndex, fileBytes.length);
                fileChunks.add(curChunkIndex, chunk);
                curChunkStartIndex += bytesPerChunk;
                ++curChunkIndex;
            }

            // get name of file without extension for naming chunks
            int dotIndex = fileName.indexOf('.');
            String fileNameNoExtension = fileName.substring(0, dotIndex);

            // write all chunks to individual files
            for (int i = 0; i < fileChunks.size(); ++i) {
                System.out.println("Created chunk " + i);
                FileOutputStream outToFileLocation = new FileOutputStream(folderPath + "/" + fileNameNoExtension + "_chunk" + i + ".bin");
                outToFileLocation.write(fileChunks.get(i));
                outToFileLocation.flush();
                outToFileLocation.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return numChunks;
    }
    public static void sendBytesToClient(OutputStream outToClient, byte[] bytes, boolean notifyConsole) {
        try {
            outToClient.write(bytes);
            outToClient.flush();
            if (notifyConsole) System.out.println("Sent \"" + bytes + "\" to client");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    public static void sendMessageToClient(OutputStream outToClient, String message) {
        sendBytesToClient(outToClient, message.getBytes(), false);

        System.out.println("Sent \"" + message + "\" to client");
    }
    // message sending method for group harmonization encoded in UTF:
    public static void sendMessageToClient(DataOutputStream outToClient, String message) {
        try {
            // new method of encoding in UTF for group harmonization
            outToClient.writeUTF(message);
        } catch(IOException e) {
            e.printStackTrace();
        }

        System.out.println("Sent \"" + message + "\" to client");
    }
    public static void sendChunkToClient(OutputStream outToClient, int chunkIndex, byte[] chunkBytes) {
        // send chunk index to client
        sendBytesToClient(outToClient, toBytes(chunkIndex), false);

        // send chunk size to client
        sendBytesToClient(outToClient, toBytes(chunkBytes.length), false);

        // send chunk to client
        sendBytesToClient(outToClient, chunkBytes, false);
    }

    public static byte[] getChunk(String folderPath, String fileName, int chunkIndex) {
        int dotIndex = fileName.indexOf('.');
        String fileNameNoExtension = fileName.substring(0, dotIndex);

        String chunkFilePath = folderPath + "/" + fileNameNoExtension + "_chunk" + chunkIndex + ".bin";
        File chunkFile = new File(chunkFilePath);

        if (!chunkFile.exists()) {
            System.out.println("getChunk(): could not get chunk number: " + chunkIndex);
            return null;
        }

        try {
            FileInputStream fileIn = new FileInputStream(chunkFile);
            byte[] chunkBytes = new byte[(int)chunkFile.length()];
            fileIn.read(chunkBytes);
            fileIn.close();

            return chunkBytes;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
    public static void sendChunksToClient(OutputStream outToClient, InputStream inFromClient, String folderPath, String fileName, List<Integer> chunkIndices) {
        for (int i = 0; i < chunkIndices.size(); ++i) {
            byte[] chunkBytes = getChunk(folderPath, fileName, chunkIndices.get(i));

            sendChunkToClient(outToClient, chunkIndices.get(i), chunkBytes);

            if (i + 1 <  chunkIndices.size()) {
                String clientResponse = receiveMessageFromClient(inFromClient);
                if (!clientResponse.equals("next")) {
                    System.out.println("Expected \"next\" but received unexpected response from client to chunk number " + i + ": " + clientResponse);
                }
            }
        }
    }
    // group harmonized version using UTF encoded "next" reading
    public static void sendChunksToClient(DataOutputStream outToClient, DataInputStream inFromClient, String folderPath, String fileName, List<Integer> chunkIndices) {
        for (int i = 0; i < chunkIndices.size(); ++i) {
            byte[] chunkBytes = getChunk(folderPath, fileName, chunkIndices.get(i));

            sendChunkToClient(outToClient, chunkIndices.get(i), chunkBytes);

            if (i + 1 <  chunkIndices.size()) {
                String clientResponse = receiveMessageFromClient(inFromClient);
                if (!clientResponse.equals("next")) {
                    System.out.println("Expected \"next\" but received unexpected response from client to chunk number " + i + ": " + clientResponse);
                }
            }
        }
    }
    public static void sendFileToClient(OutputStream outToClient, InputStream inFromClient, String filePath, int numChunks) {
        try {
            File file = new File(filePath);

            byte[] fileBytes = Files.readAllBytes(file.toPath());
            System.out.println("Total bytes in file: " + fileBytes.length);

            int bytesPerChunk = fileBytes.length / numChunks;
            if (fileBytes.length - (bytesPerChunk * (numChunks - 1)) > numChunks) {
                ++bytesPerChunk;
            }
            System.out.println("Bytes per chunk: " + bytesPerChunk);

            int curChunkStartIndex = 0;
            int curChunkIndex = 0;

            List<byte[]> fileChunks = new ArrayList<byte[]>();
            List<Integer> chunkIndicesToSend = new ArrayList<Integer>();

            // break full fileBytes array into chunks and add them to fileChunks
            while (curChunkIndex < (numChunks - 1)) {
                byte[] chunk = Arrays.copyOfRange(fileBytes, curChunkStartIndex, (curChunkStartIndex + bytesPerChunk));
                fileChunks.add(curChunkIndex, chunk);
                chunkIndicesToSend.add(curChunkIndex);
                curChunkStartIndex += bytesPerChunk;
                ++curChunkIndex;
            }

            // add last chunk (which may have fewer bytes than the others)
            System.out.println("Bytes in last chunk: " + (fileBytes.length - curChunkStartIndex));
            if (curChunkStartIndex < fileBytes.length) {
                byte[] chunk = Arrays.copyOfRange(fileBytes, curChunkStartIndex, fileBytes.length);
                fileChunks.add(curChunkIndex, chunk);
                chunkIndicesToSend.add(curChunkIndex);
                curChunkStartIndex += bytesPerChunk;
                ++curChunkIndex;
            }

            // send chunks in random order
            Random random = new Random();
            int randomIndexIndex = 0;
            int randomChunkIndex = 0;

            while (chunkIndicesToSend.size() > 0) {
                randomIndexIndex = random.nextInt(chunkIndicesToSend.size());
                randomChunkIndex = chunkIndicesToSend.get(randomIndexIndex);
                sendChunkToClient(outToClient, randomChunkIndex, fileChunks.get(randomChunkIndex));
                chunkIndicesToSend.remove(randomIndexIndex);

                if (chunkIndicesToSend.size() > 0) {
                    String clientResponse = receiveMessageFromClient(inFromClient);
                    if (!clientResponse.equals("next")) {
                        System.out.println("Expected \"next\" but received unexpected response from client to chunk number " + randomChunkIndex + ": " + clientResponse);
                    }
                }
            }

            System.out.println("Sent " + fileChunks.size() + " chunks to the client");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    public static String receiveMessageFromClient(InputStream inFromClient) {
        byte[] buffer = new byte[1024];
        int bytesRead = 0;

        try {
            bytesRead = inFromClient.read(buffer);

            if (bytesRead > 0) {
                return new String(buffer, 0, bytesRead);
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    // new harmonized version for group receives UTF encoded message
    public static String receiveMessageFromClient(DataInputStream inFromClient) {
        String receivedMsg = "";
        try {
            receivedMsg = new String(inFromClient.readUTF());
        } catch(IOException e) {
            e.printStackTrace();
        }

        return receivedMsg;
    }
    public static int receiveIntFromClient(InputStream inFromClient) {
        byte[] buffer = new byte[Integer.BYTES];
        int bytesRead = 0;

        try {
            bytesRead = inFromClient.read(buffer);

            if (bytesRead > 0) {
                ByteBuffer wrappedBuffer = ByteBuffer.wrap(buffer);
                return wrappedBuffer.getInt();
            } else {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
    public static boolean fileExists(String filesFolderPath, String fName) {
        File folder = new File(filesFolderPath);

        String[] files = folder.list();

        if (files != null) {
            for (int i = 0; i < files.length; ++i) {
                if (fName.equals(files[i])) {
                    System.out.println("Found file: " + fName);
                    return true;
                }
            }
        }

        return false;
    }

    public static class ClientThread implements Runnable {
        private Socket connectionSocket;
        private String filesFolderPath = "";
        private List<Integer> chunkIndices;

        public ClientThread(Socket connectionSocket, String filesFolderPath, List<Integer> chunkIndices) {
            this.connectionSocket = connectionSocket;
            this.filesFolderPath = filesFolderPath;
            this.chunkIndices = new ArrayList<>(chunkIndices);
        }

        @Override
        public void run() {
            try {
                InputStream baseIn = connectionSocket.getInputStream();
                DataInputStream in = new DataInputStream(new BufferedInputStream(baseIn));
                OutputStream baseOut = connectionSocket.getOutputStream();
                DataOutputStream out = new DataOutputStream(baseOut);

                // check for "rdy" message from client
                String clientMessage = receiveMessageFromClient(in);

                if (validateClientMessage(connectionSocket, clientMessage, "rdy")) {
                    // send "rdy" response to client once "rdy" message from client is received
                    sendMessageToClient(out, "rdy");

                    // receive file name message from client
                    String fName = receiveMessageFromClient(in);
                    System.out.println("Received file name from client: " + fName);

                    // send file name back to client
                    sendMessageToClient(out, fName);

                    // check for number of chunks request from client
                    String numChunksRequest = receiveMessageFromClient(in);
                    System.out.println("Received request for number of chunks from client: " + numChunksRequest);

                    sendBytesToClient(out, toBytes(chunkIndices.size()), false);
                    System.out.println("Sent number of chunks to client: " + chunkIndices.size());

                    // check for "rdyD" message from client
                    String clientMessage2 = receiveMessageFromClient(in);

                    if (validateClientMessage(connectionSocket, clientMessage2, "rdyD")) {
                        // send file if it exists
                        if (fileExists(filesFolderPath, fName)) {
                            sendChunksToClient(out, in, filesFolderPath, fName, chunkIndices);
                            System.out.println("Sent " + chunkIndices.size() + " chunks to client");

                            String clientMessage3 = receiveMessageFromClient(in);

                            if (validateClientMessage(connectionSocket, clientMessage3, "close")) {
                                connectionSocket.close();
                                System.out.println("Closed connection");
                            }
                        } else {
                            errorCloseConnection(connectionSocket, "A file with the name " + fName + " does not exist in the current files folder");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args) {
        try {
            int serverPort = 12000;
            ServerSocket serverSocket = new ServerSocket(serverPort);

            String filesFolderPath = "/Users/mia/IdeaProjects/tcp_server_stage4_group_version/files";

            String fName = "testFile2.zip";

//            int numChunks = 250;
            int numPeers = 5;

            int numChunks = createChunksTenKB(filesFolderPath, fName);

            List<List<Integer>> peerChunkIndices = new ArrayList<List<Integer>>();

            // list of all chunk indices to use for randomly distributing chunks among peers
            List<Integer> allIndices = new ArrayList<>();
            for (int i = 0; i < numChunks; ++i) {
                allIndices.add(i);
            }

            // randomly divide indices among peers except last peer
            Random randomizer = new Random();

            int chunksPerPeer = numChunks/numPeers;
            for (int i = 0; i < numPeers - 1; ++i) {
                List<Integer> curPeerChunks = new ArrayList<>();
                for (int j = 0; j < chunksPerPeer; ++j) {
                    int index = randomizer.nextInt(allIndices.size());
                    curPeerChunks.add(allIndices.get(index));
                    allIndices.remove(index);
                }
                peerChunkIndices.add(curPeerChunks);
            }

            // all remaining chunk indices go to the last peer
            List<Integer> lastPeerChunks = new ArrayList<>();
            for (int i = 0; i < allIndices.size(); ++i) {
                lastPeerChunks.add(allIndices.get(i));
            }
            peerChunkIndices.add(lastPeerChunks);

            System.out.println("Assigned " + lastPeerChunks.size() + " chunks to the last peer and " + chunksPerPeer + " chunks to each other peer");

            int curPeerCount = 0;

            System.out.println("The server is ready to receive");

            while (true) {
                // accept connection from client
                Socket connectionSocket = serverSocket.accept();

                System.out.println("Connected with peer number " + (curPeerCount + 1));

                // create a thread for client to send it a set of chunks
                if (curPeerCount < numPeers) {
                    ClientThread clientThread = new ClientThread(connectionSocket, filesFolderPath, peerChunkIndices.get(curPeerCount));
                    Thread thread = new Thread(clientThread);
                    ++curPeerCount;
                    thread.start();
                } else {
                    System.out.println("5 peers have already been connected so the new connection was denied");
                    connectionSocket.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}