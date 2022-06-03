{
  outputs = { self, nixpkgs, flake-utils }: # list out the dependencies
    let
      system = "x86_64-linux";
      npkgs = import nixpkgs {inherit system;};
      name = "resounding";
      set  = "javaPackages";

# sub-dependencies
deps = p: with p.${set}; [
  jdk gradle
];

      overlay = self: super: {

# build process
${name} = with npkgs; stdenv.mkDerivation {
  name = name;
  src = ./.;
  nativeBuildInputs = deps npkgs;
  buildPhase = "gradle";
};

      };
    in {inherit overlay;} // flake-utils.lib.eachDefaultSystem (system: # leverage flake-utils
      let
        pkgs = import nixpkgs {
          inherit system;
          overlays = [ overlay ];
        };
      in {
        defaultPackage = pkgs.${name};
        devShell = pkgs.mkShell { # development environment
          packages = p: [ p.${name} ];
          buildInputs = deps pkgs;
        };
      });
}
