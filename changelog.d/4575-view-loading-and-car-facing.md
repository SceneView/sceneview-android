<!-- category: Fixed -->
- **Web `/view`**: the stage shows the studio backdrop until the model is drawn, never a black frame. Every load opens on the model's front three-quarter, whatever the download time. `loadModel(url, { frontYaw })` handles files modelled facing away from glTF's +Z, and the catalogue uses it for the MINI, the phones, the iPad and the Citroën van (#4575).
