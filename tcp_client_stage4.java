import java.io.*;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.nio.ByteBuffer;

public class tcp_client_stage4 {
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
    public static boolean validateServerMessage(Socket socket, String receivedMsg, String expectedMsg) {
        if (receivedMsg == null) receivedMsg = "null";
        receivedMsg = receivedMsg.trim();
        if (receivedMsg.equals(expectedMsg)) {
            System.out.println("Received server message: " + receivedMsg);
            return true;
        } else {
            errorCloseConnection(socket, "Expected \"" + expectedMsg + "\" from server but received \"" + receivedMsg + "\"");
            return false;
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
    public static boolean validateServerInteger(Socket socket, Integer receivedInteger, Integer expectedInteger) {
        if (receivedInteger.equals(expectedInteger)) {
            System.out.println("Received server integer: " + receivedInteger);
            return true;
        } else {
            errorCloseConnection(socket, "Expected \"" + expectedInteger + "\" from server but received \"" + receivedInteger + "\"");
            return false;
        }
    }
    public static void sendBytesToServer(OutputStream outToServer, byte[] bytes, boolean notifyConsole) {
        try {
            outToServer.write(bytes);
            outToServer.flush();
            if (notifyConsole) System.out.println("Sent \"" + bytes + "\" to server");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    public static void sendMessageToServer(OutputStream outToServer, String message) {
        sendBytesToServer(outToServer, message.getBytes(), false);
        System.out.println("Sent \"" + message + "\" to server");
    }
    // group harmonized version using UTF encoding
    public static void sendMessageToServer(DataOutputStream outToServer, String message) {
        try {
            // new method of encoding in UTF for group harmonization
            outToServer.writeUTF(message);
        } catch(IOException e) {
            e.printStackTrace();
        }

        System.out.println("Sent \"" + message + "\" to server");
    }
    public static void sendMessageToClient(OutputStream outToClient, String message) {
        sendBytesToServer(outToClient, message.getBytes(), false);
        System.out.println("Sent \"" + message + "\" to client");
    }
    // group harmonized version using UTF encoding
    public static void sendMessageToClient(DataOutputStream outToClient, String message) {
        try {
            // new method of encoding in UTF for group harmonization
            outToClient.writeUTF(message);
        } catch(IOException e) {
            e.printStackTrace();
        }

        System.out.println("Sent \"" + message + "\" to client");
    }
    public static String receiveMessage(InputStream in) {
        byte[] buffer = new byte[1024];
        int bytesRead = 0;

        try {
            bytesRead = in.read(buffer);

            if (bytesRead > 0) {
                String message = new String(buffer, 0, bytesRead);
                return message;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    // new harmonized version for group receives UTF encoded message
    public static String receiveMessage(DataInputStream in) {
        String receivedMsg = "";
        try {
            receivedMsg = new String(in.readUTF());
        } catch(IOException e) {
            e.printStackTrace();
        }

        return receivedMsg;
    }
    public static byte[] receiveBytes(InputStream in) {
        byte[] buffer = new byte[1024];
        int bytesRead = 0;

        try {
            bytesRead = in.read(buffer);

            if (bytesRead > 0) {
                byte[] actualBytesReceived = new byte[bytesRead];
                System.arraycopy(buffer, 0, actualBytesReceived, 0, bytesRead);
                return actualBytesReceived;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static int receiveInt(InputStream in) {
        byte[] buffer = new byte[Integer.BYTES];
        int bytesRead = 0;

        try {
            bytesRead = in.read(buffer);

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

    public static void receiveChunksFromServer(InputStream inFromServer, OutputStream outToServer, String folderPath, String fileName, int numChunks) {
        try {
            int dotIndex = fileName.indexOf('.');
            String fileNameNoExtension = fileName.substring(0, dotIndex);

            int receivedChunks = 0;

            while (receivedChunks < numChunks) {
                int chunkIndex = receiveInt(inFromServer);
                System.out.println("Chunk number " + chunkIndex);

                int chunkSize = receiveInt(inFromServer);
                System.out.println("Number of bytes: " + chunkSize);

                byte[] buffer = new byte[chunkSize];
                int chunkBytesRead = 0;

                try {
                    FileOutputStream outToFileLocation = new FileOutputStream(folderPath + "/" + fileNameNoExtension + "_chunk" + chunkIndex + ".bin");
                    BufferedOutputStream bufferedOutToFileLocation = new BufferedOutputStream(outToFileLocation);

                    while (chunkBytesRead < chunkSize) {
                        int newBytesRead = inFromServer.read(buffer, chunkBytesRead, chunkSize - chunkBytesRead);

                        if (newBytesRead == -1) {
                            System.out.println("Chunk number " + chunkIndex + "was not properly received. Not enough bytes were sent or received.");
                            break;
                        }

                        chunkBytesRead += newBytesRead;
                    }

                    bufferedOutToFileLocation.write(buffer, 0, chunkBytesRead);

                    bufferedOutToFileLocation.flush();
                    bufferedOutToFileLocation.close();
                    outToFileLocation.flush();
                    outToFileLocation.close();
                } catch(IOException e) {
                    e.printStackTrace();
                }

                ++receivedChunks;
                if (receivedChunks < numChunks) sendMessageToServer(outToServer, "next");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // group harmonized version using UTF encoding for sending "next"
    public static void receiveChunksFromServer(DataInputStream inFromServer, DataOutputStream outToServer, String folderPath, String fileName, int numChunks) {
        try {
            int dotIndex = fileName.indexOf('.');
            String fileNameNoExtension = fileName.substring(0, dotIndex);

            int receivedChunks = 0;

            while (receivedChunks < numChunks) {
                int chunkIndex = receiveInt(inFromServer);
                System.out.println("Chunk number " + chunkIndex);

                int chunkSize = receiveInt(inFromServer);
                System.out.println("Number of bytes: " + chunkSize);

                byte[] buffer = new byte[chunkSize];
                int chunkBytesRead = 0;

                try {
                    FileOutputStream outToFileLocation = new FileOutputStream(folderPath + "/" + fileNameNoExtension + "_chunk" + chunkIndex + ".bin");
                    BufferedOutputStream bufferedOutToFileLocation = new BufferedOutputStream(outToFileLocation);

                    while (chunkBytesRead < chunkSize) {
                        int newBytesRead = inFromServer.read(buffer, chunkBytesRead, chunkSize - chunkBytesRead);

                        if (newBytesRead == -1) {
                            System.out.println("Chunk number " + chunkIndex + "was not properly received. Not enough bytes were sent or received.");
                            break;
                        }

                        chunkBytesRead += newBytesRead;
                    }

                    bufferedOutToFileLocation.write(buffer, 0, chunkBytesRead);

                    bufferedOutToFileLocation.flush();
                    bufferedOutToFileLocation.close();
                    outToFileLocation.flush();
                    outToFileLocation.close();
                } catch(IOException e) {
                    e.printStackTrace();
                }

                ++receivedChunks;
                if (receivedChunks < numChunks) sendMessageToServer(outToServer, "next");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void reassembleChunks(String folderPath, String fileName, int numChunks) {
        int dotIndex = fileName.indexOf('.');
        String fileNameNoExtension = fileName.substring(0, dotIndex);

        try {
            FileOutputStream fileOut = new FileOutputStream(folderPath + "/" + fileName);

            while (readingChunks) {
                // do nothing until no one is reading chunks
            }

            readingChunks = true;

            for (int i = 0; i < numChunks; ++i) {
                String chunkFilePath = folderPath + "/" + fileNameNoExtension + "_chunk" + i + ".bin";
                File chunkFile = new File(chunkFilePath);

                if (!chunkFile.exists()) {
                    System.out.println("Could not find chunk number: " + i);
                    return;
                }

                try {
                    FileInputStream fileIn = new FileInputStream(chunkFile);
                    byte[] chunkBytes = new byte[(int) chunkFile.length()];
                    fileIn.read(chunkBytes);
                    fileIn.close();

                    readingChunks = false;

                    fileOut.write(chunkBytes);
                } catch (IOException e) {
                    System.out.println("Error reading chunk number" + i + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            System.out.println("Finished reassembling chunks");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static byte[] readInChunk(String folderPath, String fileName, int chunkID) {
        try {
            int dotIndex = fileName.indexOf('.');
            String fileNameNoExtension = fileName.substring(0, dotIndex);
            String chunkFilePath = folderPath + "/" + fileNameNoExtension + "_chunk" + chunkID + ".bin";

            while (readingChunks) {
                // do nothing until no one is reading chunks
            }

            readingChunks = true;

            File chunkFile = new File(chunkFilePath);
            FileInputStream fileIn = new FileInputStream(chunkFile);
            byte[] chunkBytes = new byte[(int) chunkFile.length()];
            fileIn.read(chunkBytes);
            fileIn.close();

            readingChunks = false;
            return chunkBytes;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static void createSummary(String folderPath, String fileName, int totalNumChunks) {
        int dotIndex = fileName.indexOf('.');
        String fileNameNoExtension = fileName.substring(0, dotIndex);
        String summaryFileName = fileNameNoExtension + "_summary.txt";

        // string to contain summary of received chunks
        // (length = numChunks, each 0 signifies the chunk with that index has not been received
        // while each 1 indicates that it has)
        String summary = "";

        while (writingToSummary || readingSummary) {
            // do nothing until no one is writing to or reading summary so can safely write to file
        }

        writingToSummary = true;

        File directory = new File(folderPath);

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();

            int[] receivedChunks = new int[totalNumChunks];

            if (files != null) {
                for (int i = 0; i < files.length; ++i) {
                    if (files[i].isFile() && files[i].getName().contains(fileNameNoExtension) && (!files[i].getName().contains("summary"))) {
                        int chunkNumberStartIndex = files[i].getName().indexOf(fileNameNoExtension) + (fileNameNoExtension + "_chunk").length();
                        int chunkNumberEndIndex = files[i].getName().indexOf('.', chunkNumberStartIndex);
                        String chunkNumberString = files[i].getName().substring(chunkNumberStartIndex, chunkNumberEndIndex);
                        int chunkNumber = Integer.parseInt(chunkNumberString);
                        receivedChunks[chunkNumber] = 1;
                    }
                }

                for (int i = 0; i < receivedChunks.length; ++i) {
                    summary += receivedChunks[i];
                }

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath + "/" + summaryFileName, false))) {
                    writer.write(summary);
                    writer.flush();
                    writer.close();
                    writingToSummary = false;
                    System.out.println("Created summary file " + summaryFileName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("createSummary(): the provided folderPath, " + folderPath +", is not a directory so a summary could not be created");
        }
    }
    public static String readInSummary(String folderPath, String fileName) {
        int dotIndex = fileName.indexOf('.');
        String fileNameNoExtension = fileName.substring(0, dotIndex);
        String summaryFileName = fileNameNoExtension + "_summary.txt";

        while (writingToSummary || readingSummary) {
            // do nothing until no one is writing to or reading summary so can safely read file
        }

        readingSummary = true;
        try (BufferedReader reader = new BufferedReader(new FileReader(folderPath + "/" + summaryFileName))) {
            String line = "";
            String summary = "";
            while ((line = reader.readLine()) != null) {
                summary += line;
            }
            reader.close();
            readingSummary = false;
            return summary;
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return "";
        }
    }
    private static volatile boolean writingToSummary = false;
    private static volatile boolean readingSummary = false;
    private static volatile boolean readingChunks = false;
    public static class UploadThread implements Runnable {
        private ServerSocket uploadServerSocket;
        private Socket connectionSocket;
        private String filesFolderPath = "";
        private String fileName = "";

        public UploadThread(ServerSocket uploadServerSocket, String filesFolderPath, String fileName) {
            this.uploadServerSocket = uploadServerSocket;
            this.filesFolderPath = filesFolderPath;
            this.fileName = fileName;
        }

        @Override
        public void run() {
            try {
                Socket connectionSocket = uploadServerSocket.accept();
                System.out.println("UPLOAD: Connected with peer on upload socket");

                try {
                    InputStream baseIn = connectionSocket.getInputStream();
                    DataInputStream in = new DataInputStream(new BufferedInputStream(baseIn));
                    OutputStream baseOut = connectionSocket.getOutputStream();
                    DataOutputStream out = new DataOutputStream(baseOut);

                    // check for "rdy" message from peer
                    String clientMessage = receiveMessage(in);

                    if (validateClientMessage(connectionSocket, clientMessage, "rdy")) {
                        while (true) {
                            // send "rdy" response to client once "rdy" message from peer is received
                            sendMessageToClient(out, "rdy");

                            // receive "chunkIDList" message from peer
                            String chunkIDList = receiveMessage(in);
                            //if (validateClientMessage(connectionSocket, chunkIDList, "chunkIDList")) {
                                // send summary to client
                                String summary = readInSummary(filesFolderPath, fileName);
                                if (summary == "")
                                    System.out.println("UPLOAD: Error: the summary read in from the summary file is an empty string so was not sent to the client");
                                else {
                                    // translate summary into chunk ID list separated by spaces for group harmonization before sending
                                    List<Integer> chunkIDs = new ArrayList<Integer>();
                                    for (int i = 0; i < summary.length(); ++i) {
                                        if (summary.charAt(i) == '1') {
                                            chunkIDs.add(i);
                                        }
                                    }

                                    StringBuilder sb = new StringBuilder();
                                    for (int i = 0; i < chunkIDs.size(); i++) {
                                        sb.append(chunkIDs.get(i));
                                        if (i < chunkIDs.size() - 1) {
                                            sb.append(" ");
                                        }
                                    }
                                    String chunkIDSummary = sb.toString();

                                    sendMessageToClient(out, chunkIDSummary);

                                    // receive "rdyD" message from peer
                                    String rdyD = receiveMessage(in);
                                    if (validateClientMessage(connectionSocket, rdyD, "rdyD")) {
                                        // take requests for chunks and send those chunks
                                        while (true) {
                                            // receive chunkID OR "rdy"
                                            String idOrRdy = receiveMessage(in);
//                                            byte[] idOrRdy = receiveBytes(in);
                                            int chunkID = -1;

                                            if (!(idOrRdy.equals("rdy"))) {
                                                try {
                                                    chunkID = Integer.parseInt(idOrRdy);
                                                    System.out.println("UPLOAD: Received request for chunk with ID: " + chunkID);

                                                } catch (Exception e) {
                                                    // if not an integer, then is ready message so exit chunk sending loop
                                                    String message = new String(idOrRdy);
                                                    System.out.println("UPLOAD: Received message from peer: " + message);
                                                    break;
                                                }
                                            } else {
                                                // if not an integer, then is ready message so exit chunk sending loop
                                                String message = new String(idOrRdy);
                                                System.out.println("UPLOAD: Received message from peer: " + message);
                                                break;
                                            }

                                            // read in chunk with that ID
                                            byte[] chunk = readInChunk(filesFolderPath, fileName, chunkID);
                                            if (chunk != null) {
                                                // send chunk size
                                                sendBytesToServer(out, toBytes(chunk.length), false);
                                                System.out.println("UPLOAD: Sent chunk size " + chunk.length + " to the peer");
                                                // send chunk
                                                sendBytesToServer(out, chunk, false);
                                                System.out.println("UPLOAD: Sent chunk with ID " + chunkID + " to the peer");
                                            } else {
                                                System.out.println("UPLOAD: The chunk with ID " + chunkID + " was read in as null so the chunk was not sent to the peer");
                                            }
                                        }
                                    }
                                }
                            //}
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static class DownloadThread implements Runnable {
        private Socket connectionSocket;
        private String filesFolderPath = "";
        private String fileName = "";

        public DownloadThread(Socket connectionSocket, String filesFolderPath, String fileName) {
            this.connectionSocket = connectionSocket;
            this.filesFolderPath = filesFolderPath;
            this.fileName = fileName;
        }

        @Override
        public void run() {
            try {
                InputStream baseIn = connectionSocket.getInputStream();
                DataInputStream in = new DataInputStream(new BufferedInputStream(baseIn));
                OutputStream baseOut = connectionSocket.getOutputStream();
                DataOutputStream out = new DataOutputStream(baseOut);

                // get current self summary
                String selfSummary = readInSummary(filesFolderPath, fileName);

                boolean receivedAllChunks = true;
                for (int i = 0; i < selfSummary.length(); ++i) {
                    if (selfSummary.charAt(i) == '0') {
                        receivedAllChunks = false;
                        break;
                    }
                }

                while (!receivedAllChunks) {
                    // send "rdy" message to peer
                    sendMessageToServer(out, "rdy");

                    // check for "rdy" message from peer
                    String rdy = receiveMessage(in);

                    if (validateClientMessage(connectionSocket, rdy, "rdy")) {
                        // send "chunkIDList" message to peer
                        sendMessageToServer(out,"chunkIDList");

                        // receive summary from peer
                        String peerSummary = receiveMessage(in);

                        // get an array of the string chunk IDs
                        String[] peerSummaryArray = peerSummary.split(" ");

                        System.out.println("DOWNLOAD: Received summary: " + peerSummary);

                        // identify missing chunks in selfSummary that are in peerSummary
                        List<Integer> missingChunks = new ArrayList<Integer>();
                        for (int i = 0; i < peerSummaryArray.length; ++i) {
                            int chunkIndex = Integer.parseInt(peerSummaryArray[i]);
                            if (selfSummary.charAt(chunkIndex) == '0') {
                                missingChunks.add(Integer.parseInt(peerSummaryArray[i]));
                            }
                        }

                        // send "rdyD" message to peer to signify readiness to download chunks
                        sendMessageToServer(out, "rdyD");

                        // request all missing chunks
                        for (int i = 0; i < missingChunks.size(); ++i) {
                            // send chunkID to peer
//                            sendBytesToServer(out, toBytes(missingChunks.get(i)), false);
                            sendMessageToServer(out, Integer.toString(missingChunks.get(i)));
                            System.out.println("DOWNLOAD: Requested chunk " + missingChunks.get(i) + " from peer");

                            // receive chunk size from peer
                            int chunkSize = receiveInt(in);

                            // receive chunk from peer
                            byte[] buffer = new byte[chunkSize];
                            int chunkBytesRead = 0;

                            try {
                                int dotIndex = fileName.indexOf('.');
                                String fileNameNoExtension = fileName.substring(0, dotIndex);
                                FileOutputStream outToFileLocation = new FileOutputStream(filesFolderPath + "/" + fileNameNoExtension + "_chunk" + missingChunks.get(i) + ".bin");
                                BufferedOutputStream bufferedOutToFileLocation = new BufferedOutputStream(outToFileLocation);

                                while (chunkBytesRead < chunkSize) {
                                    int newBytesRead = in.read(buffer, chunkBytesRead, chunkSize - chunkBytesRead);

                                    if (newBytesRead == -1) {
                                        System.out.println("DOWNLOAD: Chunk number " + missingChunks.get(i) + "was not properly received. Not enough bytes were sent or received.");
                                        break;
                                    }

                                    chunkBytesRead += newBytesRead;
                                }

                                System.out.println("DOWNLOAD: Received chunk " + missingChunks.get(i));

                                bufferedOutToFileLocation.write(buffer, 0, chunkBytesRead);

                                bufferedOutToFileLocation.flush();
                                bufferedOutToFileLocation.close();
                                outToFileLocation.flush();
                                outToFileLocation.close();

                                // update the summary file each time a chunk is saved to disk
                                createSummary(filesFolderPath, fileName, selfSummary.length());
                            } catch(IOException e) {
                                e.printStackTrace();
                            }
                        }
                        // check if all chunks have been received
                        selfSummary = readInSummary(filesFolderPath, fileName);
                        receivedAllChunks = true;
                        for (int i = 0; i < selfSummary.length(); ++i) {
                            if (selfSummary.charAt(i) == '0') {
                                receivedAllChunks = false;
                                break;
                            }
                        }
                    }
                }
                System.out.println("DOWNLOAD: Reassembling chunks");
                reassembleChunks(filesFolderPath, fileName, selfSummary.length());
                System.out.println("DOWNLOAD: finished");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args) {
        try {
            String serverName = "127.0.0.1";
            int serverPort = 12000;
            Socket clientSocket = new Socket(serverName, serverPort);

            String filesFolderPath = "/Users/mia/IdeaProjects/tcp_client_stage4_group_version/files";

            InputStream baseIn = clientSocket.getInputStream();
            DataInputStream in = new DataInputStream(new BufferedInputStream(baseIn));
            OutputStream baseOut = clientSocket.getOutputStream();
            DataOutputStream out = new DataOutputStream(baseOut);

            Scanner scanner = new Scanner(System.in);

            System.out.print("Input total number of chunks: ");
            int totalNumChunks = Integer.parseInt(scanner.nextLine());

            // let the server know the client is ready
            sendMessageToServer(out, "rdy");

            // read message from the server and check that the server is ready
            String serverMessage_rdy = receiveMessage(in);

            if(validateServerMessage(clientSocket, serverMessage_rdy, "rdy")) {
                // once server is ready, read in the name of the file the user wants from the console
                System.out.print("Input file name: ");
                String fName = scanner.nextLine();

                // send the file name to the server
                sendMessageToServer(out, fName);

                // read message from the server and check that it is the name of the file
                String serverMessage_fName = receiveMessage(in);

                if (validateServerMessage(clientSocket, serverMessage_fName, fName)) {
                    // send request for number of chunks to server
                    sendMessageToServer(out, "Requesting number of chunks");
                    System.out.println("Sent request for number of chunks to server");

                    // read integer from the server and check that it is the same number of chunks as requested
                    int numChunks = receiveInt(in);
                    System.out.println("Received number of chunks from server: " + numChunks);

                        // send readyD message to server
                        sendMessageToServer(out, "rdyD");

                        // receive chunks from server if it exists
                        if (!clientSocket.isClosed()) {
                            receiveChunksFromServer(in, out, filesFolderPath, fName, numChunks);

                            // tell the server to close the connection
                            sendMessageToServer(out, "close");
                        } else {
                            System.out.println("A file with the given name does not exist on the server");
                        }

                        try {
                            createSummary(filesFolderPath, fName, totalNumChunks);

                            // get IP address and port number of peer to download from (from terminal)
                            System.out.print("Input IP address of peer to download from: ");
                            String peerServerName = scanner.nextLine();
                            System.out.print("Input port number of peer to download from: ");
                            int downloadPort = Integer.parseInt(scanner.nextLine());

                            int uploadPort = 12002;
                            ServerSocket uploadSocket = new ServerSocket(uploadPort);

                            // create a thread send peer chunks
                            UploadThread uploadThread = new UploadThread(uploadSocket, filesFolderPath, fName);
                            Thread ut = new Thread(uploadThread);
                            ut.start();

                            Socket downloadConnectionSocket = null;

                            System.out.println("Searching for server peer");
                            while(downloadConnectionSocket == null) {
                                try {
                                    downloadConnectionSocket = new Socket(peerServerName, downloadPort);
                                } catch (ConnectException e) {
                                    downloadConnectionSocket = null;
                                }
                            }

                            // create a thread receive peer chunks
                            DownloadThread downloadThread = new DownloadThread(downloadConnectionSocket, filesFolderPath, fName);
                            Thread dt = new Thread(downloadThread);
                            dt.start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        while (true) {
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                }
            }

            scanner.close();
            in.close();
            out.close();
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}